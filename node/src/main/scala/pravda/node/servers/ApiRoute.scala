package pravda.node

package servers

import java.util.Base64

import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import cats.Show
import com.google.protobuf.ByteString
import pravda.common.bytes
import pravda.node.clients.AbciClient
import pravda.node.data.blockchain.Transaction.SignedTransaction
import pravda.node.data.blockchain.TransactionData
import pravda.common.domain.{Address, NativeCoins}
import pravda.node.data.serialization.json._

import scala.concurrent.duration._
import scala.util.Random

class ApiRoute(abciClient: AbciClient) {

  import pravda.node.utils.AkkaHttpSpecials._

  val hexUnmarshaller: Unmarshaller[String, ByteString] =
    Unmarshaller.strict(hex => bytes.hex2byteString(hex))

  val bigDecimalUnmarshaller: Unmarshaller[String, BigDecimal] =
    Unmarshaller.strict(s => BigDecimal(s))

  val intUnmarshaller: Unmarshaller[String, Int] =
    Unmarshaller.strict(s => s.toInt)

  def bodyToTransactionData(body: HttpEntity.Strict): TransactionData = {
    body.contentType.mediaType match {
      case MediaTypes.`application/octet-stream` =>
        TransactionData @@ ByteString.copyFrom(body.data.toArray)
      case MediaTypes.`application/base64` =>
        val bytes = Base64.getDecoder.decode(body.data.toArray)
        TransactionData @@ ByteString.copyFrom(bytes)
      case mediaType =>
        throw new IllegalArgumentException(s"Unsupported Content-Type: $mediaType")
    }
  }

  val route: Route =
    pathPrefix("public") {
      post {
        withoutRequestTimeout {
          path("broadcast") {
            parameters(
              ('from.as(hexUnmarshaller),
               'signature.as(hexUnmarshaller),
               'nonce.as(intUnmarshaller).?,
               'fee.as(bigDecimalUnmarshaller),
               'wattPrice.as(bigDecimalUnmarshaller),
               'mode.?)) { (from, signature, maybeNonce, fee, maybeMode) =>
              extractStrictEntity(1.second) { body =>
                val program = bodyToTransactionData(body)
                val nonce = maybeNonce.getOrElse(Random.nextInt())
                val tx = SignedTransaction(Address @@ from, program, signature, NativeCoins @@ fee, wattPrice, Random.nextInt)
                println(Show[SignedTransaction].show(tx))
                val mode = maybeMode.getOrElse("commit")
                val result = abciClient.broadcastTransaction(tx, mode)

                onSuccess(result) {
                  case Right(stack) => complete(stack)
                  case Left(error)  => complete(error)
                }
              }
            }
          }
        }
      }
    }
}

object ApiRoute {}
