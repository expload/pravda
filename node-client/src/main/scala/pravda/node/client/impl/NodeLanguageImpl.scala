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

package pravda.node.client.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import akka.util.{ByteString => AkkaByteString}
import cats.data.EitherT
import cats.implicits._
import com.google.protobuf.ByteString
import pravda.common.{bytes, cryptography}
import pravda.common.data.blockchain._
import pravda.node.client.NodeLanguage
import pravda.common.data.blockchain.Transaction.UnsignedTransaction
import pravda.common.data.blockchain.TransactionData
import pravda.common.data.blockchain._
import pravda.node.launcher
import pravda.common.serialization._
import pravda.node.data.serialization.json._
import pravda.vm.asm.PravdaAssembler

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Random

final class NodeLanguageImpl(implicit system: ActorSystem,
                             materializer: ActorMaterializer,
                             executionContext: ExecutionContextExecutor)
    extends NodeLanguage[Future] {

  def launch(configPath: String): Future[Unit] = Future {
    sys.props.put("config.file", configPath)
    launcher.main(Array.empty)
    Thread.currentThread().join()
  }

  def singAndBroadcastTransaction(uriPrefix: String,
                                  address: ByteString,
                                  privateKey: ByteString,
                                  wattPayerPrivateKey: Option[ByteString],
                                  wattLimit: Long,
                                  wattPrice: NativeCoin,
                                  wattPayer: Option[Address],
                                  data: ByteString): Future[Either[String, TransactionResult]] = {

    val fromHex = bytes.byteString2hex(address)
    val request = {
      val tx = UnsignedTransaction(
        from = Address @@ address,
        program = TransactionData @@ data,
        wattLimit = wattLimit,
        wattPrice = wattPrice,
        wattPayer = None,
        nonce = Random.nextInt()
      )

      val stx = {
        wattPayerPrivateKey match {
          case Some(pk) =>
            val one = cryptography.signTransaction(PrivateKey @@ privateKey, tx.copy(wattPayer = wattPayer))
            cryptography.addWattPayerSignature(PrivateKey @@ pk, one)
          case None => cryptography.signTransaction(PrivateKey @@ privateKey, tx)
        }
      }

      HttpRequest(
        method = HttpMethods.POST,
        uri = uriPrefix +
          s"?from=$fromHex" +
          s"&signature=${bytes.byteString2hex(stx.signature)}" +
          s"&nonce=${tx.nonce}" +
          s"&wattLimit=${tx.wattLimit}" +
          s"&wattPrice=${tx.wattPrice}" +
          wattPayer.fold("")(wp => s"&wattPayer=${bytes.byteString2hex(wp)}") +
          stx.wattPayerSignature.fold("")(s => s"&wattPayerSignature=${bytes.byteString2hex(s)}"),
        entity = HttpEntity(data.toByteArray)
      )
    }

    Http()
      .singleRequest(request)
      .flatMap { response =>
        response.entity.dataBytes
          .runFold(AkkaByteString.empty)(_ ++ _)
          .map { x =>
            val txr = transcode(Json @@ x.utf8String).to[Either[RpcError, TransactionResult]]
            txr.left.map(_.message)
          }
      }
  }

  def broadcastMethodCall(uriPrefix: String,
                          walletAddress: ByteString,
                          walletPrivateKey: ByteString,
                          wattPayerPrivateKey: Option[ByteString],
                          wattLimit: Long,
                          wattPrice: NativeCoin,
                          wattPayer: Option[Address],
                          programAddress: String,
                          programMethod: String,
                          programArgs: Seq[String]): Future[Either[String, TransactionResult]] = {

    def mkCallCode(address: String, method: String, args: Seq[String]): String = {
      val sb = new StringBuilder
      args.foreach(arg => sb.append(s"push $arg "))
      sb.append(s"""push "$method" """)
      sb.append(s"push $address ")
      sb.append(s"push ${args.size + 1} ")
      sb.append("pcall")
      sb.mkString
    }
    val code = mkCallCode(programAddress, programMethod, programArgs)
    val result = for {
      compiledCode <- PravdaAssembler
        .assemble(code, saveLabels = true)
        .left
        .map(s => s"Invalid asm code: ${s.mkString}")
        .toEitherT[Future]
      res <- EitherT(
        singAndBroadcastTransaction(uriPrefix,
                                    walletAddress,
                                    walletPrivateKey,
                                    wattPayerPrivateKey,
                                    wattLimit,
                                    wattPrice,
                                    wattPayer,
                                    compiledCode))
    } yield res
    result.value
  }

  def execute(data: ByteString, address: Address, endpoint: String): Future[Either[String, TransactionResult]] = {
    val fromHex = bytes.byteString2hex(address)
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"$endpoint/execute?from=$fromHex",
      entity = HttpEntity(data.toByteArray)
    )
    Http()
      .singleRequest(request)
      .flatMap { response =>
        response.entity.dataBytes
          .runFold(AkkaByteString.empty)(_ ++ _)
          .map { x =>
            val txr = transcode(Json @@ x.utf8String).to[Either[RpcError, TransactionResult]]
            txr.left.map(_.message)
          }
      }
  }
}
