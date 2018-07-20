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
import tethys.JsonReader
import tethys.derivation.semiauto.jsonReader
import pravda.node.data.serialization._
import pravda.node.data.serialization.json._

import scala.language.higherKinds

final class Broadcast[F[_]: Monad](io: IoLanguage[F], api: NodeLanguage[F], compilers: CompilersLanguage[F]) {

  import Broadcast._

  import PravdaConfig.Broadcast.Mode

  def apply(config: PravdaConfig.Broadcast): F[Unit] = {

    val readFromFile = (path: String) =>
      io.readFromFile(path)
        .map(_.toRight(s"`$path` is not found."))

    val errorOrResult: EitherT[F, String, String] =
      for {
        walletJson <- EitherT(
          config.wallet
            .fold[F[Either[String, ByteString]]](Monad[F].pure(Left("Wallet file should be defined")))(readFromFile))
        wallet = transcode(Json @@ walletJson.toStringUtf8).to[Wallet]
        program <- config.mode match {
          case Mode.Run =>
            useOption(config.input)(io.readFromStdin(), readFromFile)
          case Mode.Transfer(Some(address), Some(amount)) if bytes.isHex(address) =>
            EitherT(compilers.asm(s"push x$address bigint($amount) transfer"))
          case Mode.Transfer(Some(_), _) =>
            EitherT[F, String, ByteString](Monad[F].pure(Left("Invalid payee address")))
          case Mode.Transfer(None, _) =>
            EitherT[F, String, ByteString](Monad[F].pure(Left("Payee address should be defined")))
          case Mode.Transfer(_, None) =>
            EitherT[F, String, ByteString](Monad[F].pure(Left("Amount of native coins should be defined")))
          case Mode.Deploy =>
            useOption(config.input)(io.readFromStdin(), readFromFile).flatMap(
              input => EitherT(compilers.asm(s"push x${bytes.byteString2hex(input)} pcreate"))
            )
          case Mode.Update(Some(address)) if bytes.isHex(address) && address.length == 48 =>
            useOption(config.input)(io.readFromStdin(), readFromFile).flatMap(
              input => EitherT(compilers.asm(s"push x${bytes.byteString2hex(input)} push x$address pupdate"))
            )
          case Mode.Update(Some(_)) =>
            EitherT[F, String, ByteString](Monad[F].pure(Left("Invalid program address")))
          case Mode.Update(None) =>
            EitherT[F, String, ByteString](Monad[F].pure(Left("Program address should be defined")))
          case _ =>
            EitherT[F, String, ByteString](Monad[F].pure(Left("Broadcast mode should be selected.")))
        }
        result <- EitherT {
          api.singAndBroadcastTransaction(
            uriPrefix = config.endpoint,
            address = wallet.address,
            privateKey = wallet.privateKey,
            wattPrice = config.wattPrice,
            wattLimit = config.wattLimit,
            dryRun = config.dryRun,
            data = program
          )
        }
      } yield result

    errorOrResult.value.flatMap {
      case Left(error)   => io.writeStringToStderrAndExit(s"$error\n")
      case Right(result) => io.writeStringToStdout(s"$result\n")
    }
  }
}

object Broadcast {

  final case class Wallet(address: ByteString, privateKey: ByteString)

  implicit val walletReader: JsonReader[Wallet] = jsonReader[Wallet]
}
