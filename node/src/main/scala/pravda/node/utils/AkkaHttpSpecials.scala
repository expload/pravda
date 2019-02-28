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

package pravda.node.utils

import java.nio.charset.StandardCharsets

import akka.http.scaladsl.marshalling.{Marshaller, Marshalling, ToEntityMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenges}
import akka.http.scaladsl.server.Directives.{AuthenticationResult, authenticateOrRejectWithChallenge}
import akka.http.scaladsl.server.directives.BasicDirectives.extractExecutionContext
import akka.http.scaladsl.server.directives.{AuthenticationDirective, AuthenticationResult}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import pravda.node.data.serialization._
import pravda.node.data.cryptography.PrivateKey
import pravda.node.data.domain.Wallet
import pravda.node.data.cryptography
import pravda.node.persistence.NodeStore

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.http.scaladsl.marshalling.PredefinedToResponseMarshallers

object AkkaHttpSpecials extends PredefinedToResponseMarshallers {

  final val JsonContentTypeHeader = headers
    .`Content-Type`(ContentType(MediaTypes.`application/json`))
    .contentType

  implicit def transcodingUnmarshaller[T](implicit jtc: Transcoder[Json, T],
                                          btc: Transcoder[Protobuf, T],
                                          mat: ActorMaterializer): FromEntityUnmarshaller[T] =
    Unmarshaller { implicit ec => entity =>
      entity.toStrict(5.seconds) map {
        case strict if strict.contentType.mediaType == MediaTypes.`application/octet-stream` =>
          val bytesRaw = strict.data.toArray
          transcode(Protobuf @@ bytesRaw).to[T]
        case strict if strict.contentType.mediaType == MediaTypes.`application/json` =>
          val charset = strict.contentType.charsetOption
            .fold(StandardCharsets.UTF_8)(_.nioCharset())
          val jsonRaw = new String(strict.data.toArray, charset)
          transcode(Json @@ jsonRaw).to[T]
      }
    }

  implicit def transcodingMarshaller[T](implicit tc: Transcoder[T, Json]): ToEntityMarshaller[T] =
    Marshaller { implicit ec => value =>
      Future.successful {
        val data = ByteString(transcode(value).to[Json])
        val contentType = JsonContentTypeHeader
        val entity = HttpEntity.Strict(contentType, data)
        List(Marshalling.Opaque(() => entity))
      }
    }

  final case class OpenedWallet(wallet: Wallet, privateKey: PrivateKey)

  /** Custom directive for wallet decryption */
  def authenticateWalletAsync(realm: String)(implicit nodeStore: NodeStore): AuthenticationDirective[OpenedWallet] =
    extractExecutionContext.flatMap { implicit ec =>
      lazy val failure = AuthenticationResult.failWithChallenge(HttpChallenges.basic(realm))
      authenticateOrRejectWithChallenge[BasicHttpCredentials, OpenedWallet] { maybeCredentials =>
        maybeCredentials.fold(Future.successful[AuthenticationResult[OpenedWallet]](failure)) { credentials =>
          nodeStore.getWallet(credentials.username) map { maybeWallet =>
            maybeWallet
              .flatMap { wallet =>
                cryptography.decryptPrivateKey(wallet.privateKey, credentials.password) map { privateKey =>
                  OpenedWallet(wallet, privateKey)
                }
              }
              .map(AuthenticationResult.success)
              .getOrElse(failure)
          }
        }
      }
    }
}
