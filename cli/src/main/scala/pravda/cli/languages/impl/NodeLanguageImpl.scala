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

package pravda.cli.languages.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import akka.util.{ByteString => AkkaByteString}
import com.google.protobuf.ByteString
import pravda.cli.languages.NodeLanguage
import pravda.common.bytes
import pravda.common.domain.{Address, NativeCoin}
import pravda.node.data.blockchain.Transaction.UnsignedTransaction
import pravda.node.data.blockchain.TransactionData
import pravda.node.data.cryptography
import pravda.node.data.cryptography.PrivateKey
import pravda.node.launcher

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
                                  data: ByteString): Future[Either[String, String]] = {

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
        val one = cryptography.signTransaction(PrivateKey @@ privateKey, tx)
        wattPayerPrivateKey match {
          case Some(pk) => cryptography.addWattPayerSignature(PrivateKey @@ pk, one.copy(wattPayer = wattPayer))
          case None     => one
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
          .runFold(AkkaByteString(""))(_ ++ _)
          .map(x => Right(x.utf8String))
      }
  }
}
