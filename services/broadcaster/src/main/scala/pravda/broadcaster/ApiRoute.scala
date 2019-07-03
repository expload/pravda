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

package pravda.broadcaster

import java.util.Base64

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.google.protobuf.ByteString
import pravda.common.bytes
import pravda.common.domain.{Address, NativeCoin}
import pravda.node.client.impl.NodeLanguageImpl
import pravda.node.data.blockchain.TransactionData
import pravda.node.data.cryptography._
import pravda.node.data.serialization.json._
import pravda.node.utils.AkkaHttpSpecials._
import pravda.vm.asm.PravdaAssembler

import scala.concurrent.duration._

class ApiRoute(api: NodeLanguageImpl, url: String, publicKey: Address, secretKey: PrivateKey) {

  implicit val hexUnmarshaller: Unmarshaller[String, ByteString] =
    Unmarshaller.strict(hex => bytes.hex2byteString(hex))

  val route: Route =
    post {
      withoutRequestTimeout {
        path("broadcast") {
          parameters(('wattLimit.as[Long], 'wattPrice.as[Long])) { (wattLimit, wattPrice) =>
            extractStrictEntity(1.second) { body =>
              bodyToTransactionData(body).fold(
                errorMessage => complete((StatusCodes.BadRequest, errorMessage)),
                program => {
                  val future = api.singAndBroadcastTransaction(
                    url,
                    publicKey,
                    secretKey,
                    None,
                    wattLimit,
                    NativeCoin @@ wattPrice,
                    None,
                    program
                  )

                  onSuccess(future) {
                    case Left(s)  => complete((StatusCodes.BadRequest, s))
                    case Right(s) => complete(s)
                  }
                }
              )
            }
          }
        } ~
          path("broadcast-call") {
            parameters(
              ('address.as[String],
               'method.as[String],
               'arg.*,
               'wattLimit.as[Long] ? (100000L),
               'wattPrice.as[Long] ? (1L))) {
              case (programAddress, programMethod, programArgs, wattLimit, wattPrice) =>
                // Arguments from the query string are extracted from the end, thus we should reverse the arguments list
                val arguments = programArgs.toSeq.reverse
                val f = api.broadcastMethodCall(
                  url,
                  publicKey,
                  secretKey,
                  None,
                  wattLimit,
                  NativeCoin @@ wattPrice,
                  None,
                  programAddress,
                  programMethod,
                  arguments
                )

                onSuccess(f) {
                  case Left(s)  => complete((StatusCodes.BadRequest, s))
                  case Right(s) => complete(s)
                }
            }
          }
      }
    }

  def bodyToTransactionData(body: HttpEntity.Strict): Either[String, ByteString] = {
    body.contentType.mediaType match {
      case MediaTypes.`application/octet-stream` =>
        val data = TransactionData @@ ByteString.copyFrom(body.data.toArray)
        Right(data)
      case MediaTypes.`application/base64` =>
        val bytes = Base64.getDecoder.decode(body.data.toArray)
        val data = TransactionData @@ ByteString.copyFrom(bytes)
        Right(data)
      case MediaTypes.`text/x-asm` | MediaTypes.`text/plain` =>
        PravdaAssembler.assemble(body.data.utf8String, true).left.map(_.mkString)
      case mediaType =>
        Left("Bad media type:" + mediaType)
    }
  }
}
