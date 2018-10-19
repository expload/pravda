/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.cli.programs

import cats._
import cats.data.EitherT
import cats.implicits._
import com.google.protobuf.ByteString
import pravda.cli.PravdaConfig
import pravda.cli.languages.{CompilersLanguage, IoLanguage, NodeLanguage}
import pravda.common.bytes
import pravda.common.contrib.ed25519
import pravda.common.domain.Address
import tethys.JsonReader
import tethys.derivation.semiauto.jsonReader
import pravda.node.data.serialization._
import pravda.node.data.serialization.json._
import pravda.vm.Data
import pravda.vm.operations.SystemOperations

import scala.language.higherKinds

final class Broadcast[F[_]: Monad](io: IoLanguage[F], api: NodeLanguage[F], compilers: CompilersLanguage[F]) {

  import Broadcast._

  import PravdaConfig.Broadcast.Mode

  def apply(config: PravdaConfig.Broadcast): F[Unit] = {

    val dryRunUriPrefix = s"${config.endpoint}/dryRun"

    def extractCode(address: Address, wallet: Wallet, wattPayerWallet: Option[Wallet]): EitherT[F, String, ByteString] = {
      for {
        codeExtractor <- EitherT {
          compilers.asm(s"push x${bytes.byteString2hex(address)} code")
        }
        txr <- EitherT {
          api.singAndBroadcastTransaction(
            uriPrefix = dryRunUriPrefix,
            address = wallet.address,
            wattPayerPrivateKey = wattPayerWallet.map(_.privateKey),
            privateKey = wallet.privateKey,
            wattPrice = config.wattPrice,
            wattLimit = config.wattLimit,
            wattPayer = wattPayerWallet.map(_.address),
            data = codeExtractor
          )
        }
        // Parse response to get program code
        programCode <- EitherT.fromEither[F] {
          txr.executionResult
            .left.map(transcode(_).to[Json])
            .flatMap { fs =>
              // first item in the stack should be
              // bytes with code of program
              fs.stack.headOption
                .collect { case Data.Primitive.Bytes(b) => Right(b) }
                .getOrElse(Left(s"Unexpected response: ${transcode(fs).to[Json]}"))
            }
        }
      } yield programCode
    }

    val readFromFile = (path: String) =>
      io.readFromFile(path)
        .map(_.toRight(s"`$path` is not found."))

    val errorOrResult: EitherT[F, String, String] =
      for {
        walletJson <- EitherT(
          config.wallet
            .fold[F[Either[String, ByteString]]](Monad[F].pure(Left("Wallet file should be defined")))(readFromFile))
        wattPayerWalletJson <- EitherT(
          config.wattPayerWallet
            .map(readFromFile)
            .sequence
            .map(_.fold(Right(None): Either[String, Option[ByteString]])(_.map(Some(_))))
        )
        wallet = transcode(Json @@ walletJson.toStringUtf8).to[Wallet]
        wattPayerWallet = wattPayerWalletJson.map(json => transcode(Json @@ json.toStringUtf8).to[Wallet])
        program <- config.mode match {
          case Mode.Run =>
            useOption(config.input)(io.readFromStdin(), readFromFile)
          case Mode.Seal =>
            for {
              programWalletJson <- EitherT(config.programWallet.fold[F[Either[String, ByteString]]](Monad[F].pure(Left("Program wallet file should be defined")))(readFromFile))
              programWallet = transcode(Json @@ programWalletJson.toStringUtf8).to[Wallet]
              programCode <- extractCode(programWallet.address, wallet, wattPayerWallet)
              signatureHex = {
                val message = SystemOperations.SealTag.concat(programCode).toByteArray
                val signature = ed25519.sign(programWallet.privateKey.toByteArray, message)
                bytes.bytes2hex(signature)
              }
              sealCode <- EitherT {
                val addressHex = bytes.byteString2hex(programWallet.address)
                compilers.asm(s"push x$addressHex push x$signatureHex seal")
              }
            } yield sealCode
          case Mode.Transfer(Some(address), Some(amount)) if bytes.isHex(address) =>
            EitherT(compilers.asm(s"push x$address push bigint($amount) transfer"))
          case Mode.Transfer(Some(_), _) =>
            EitherT[F, String, ByteString](Monad[F].pure(Left("Invalid payee address")))
          case Mode.Transfer(None, _) =>
            EitherT[F, String, ByteString](Monad[F].pure(Left("Payee address should be defined")))
          case Mode.Transfer(_, None) =>
            EitherT[F, String, ByteString](Monad[F].pure(Left("Amount of native coins should be defined")))
          case Mode.Deploy =>
            for {
              programWalletJson <- EitherT(config.programWallet.fold[F[Either[String, ByteString]]](Monad[F].pure(Left("Program wallet file should be defined")))(readFromFile))
              programWallet = transcode(Json @@ programWalletJson.toStringUtf8).to[Wallet]
              input <- useOption(config.input)(io.readFromStdin(), readFromFile)
              signature = ed25519.sign(programWallet.privateKey.toByteArray, input.toByteArray)
              addressHex = bytes.byteString2hex(programWallet.address)
              programHex = bytes.byteString2hex(input)
              signatureHex = bytes.bytes2hex(signature)
              code <- EitherT(compilers.asm(s"push x$addressHex push x$programHex push x$signatureHex pcreate"))
            } yield code
          case Mode.Update =>
            for {
              programWalletJson <- EitherT(config.programWallet.fold[F[Either[String, ByteString]]](Monad[F].pure(Left("Program wallet file should be defined")))(readFromFile))
              programWallet = transcode(Json @@ programWalletJson.toStringUtf8).to[Wallet]
              newCode <- useOption(config.input)(io.readFromStdin(), readFromFile)
              oldCode <- extractCode(programWallet.address, wallet, wattPayerWallet)
              signatureHex = {
                val message = oldCode.concat(newCode).toByteArray
                val signature = ed25519.sign(programWallet.privateKey.toByteArray, message)
                bytes.bytes2hex(signature)
              }
              newCodeHex = bytes.byteString2hex(newCode)
              updateCode <- EitherT {
                val addressHex = bytes.byteString2hex(programWallet.address)
                compilers.asm(s"push x$addressHex push x$newCodeHex push x$signatureHex pupdate")
              }
            } yield updateCode
          case _ =>
            EitherT[F, String, ByteString](Monad[F].pure(Left("Broadcast mode should be selected.")))
        }
        result <- EitherT {
          api.singAndBroadcastTransaction(
            uriPrefix =
              if (config.dryRun) dryRunUriPrefix
              else s"${config.endpoint}/broadcast",
            address = wallet.address,
            wattPayerPrivateKey = wattPayerWallet.map(_.privateKey),
            privateKey = wallet.privateKey,
            wattPrice = config.wattPrice,
            wattLimit = config.wattLimit,
            wattPayer = wattPayerWallet.map(_.address),
            data = program
          )
        }
      } yield transcode(result).to[Json]

    errorOrResult.value.flatMap {
      case Left(error)   => io.writeStringToStderrAndExit(s"$error\n")
      case Right(result) => io.writeStringToStdout(s"$result\n")
    }
  }
}

object Broadcast {

  final case class Wallet(address: Address, privateKey: ByteString)

  implicit val walletReader: JsonReader[Wallet] = jsonReader[Wallet]
}
