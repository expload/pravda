package io.mytc.timechain.utils

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
import io.mytc.timechain.data.serialization._
import json._
import io.mytc.timechain.data.cryptography.PrivateKey
import io.mytc.timechain.data.domain.Wallet
import io.mytc.timechain.data.cryptography
import io.mytc.timechain.persistence.NodeStore

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.http.scaladsl.marshalling.PredefinedToResponseMarshallers

object AkkaHttpSpecials extends PredefinedToResponseMarshallers {

  final val JsonContentTypeHeader = headers
    .`Content-Type`(ContentType(MediaTypes.`application/json`))
    .contentType

  implicit def transcodingUnmarshaller[T](implicit tc: Transcoder[Json, T], mat: ActorMaterializer): FromEntityUnmarshaller[T] =
    Unmarshaller { implicit ec => entity =>
      entity.toStrict(5.seconds) map {
        case strict if strict.contentType.mediaType == MediaTypes.`application/json` =>
          val charset = strict
            .contentType
            .charsetOption
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

  case class OpenedWallet(wallet: Wallet, privateKey: PrivateKey)

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
