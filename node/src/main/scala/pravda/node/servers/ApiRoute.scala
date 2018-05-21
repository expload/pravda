package pravda.node

package servers

import java.util.Base64

import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.google.protobuf.ByteString
import pravda.node.clients.AbciClient
import pravda.node.data.blockchain.Transaction.SignedTransaction
import pravda.node.data.blockchain.TransactionData
import pravda.node.data.common.{Address, Mytc}

import scala.concurrent.duration._
import scala.util.Random

class ApiRoute(abciClient: AbciClient) {

  import pravda.node.utils.AkkaHttpSpecials._

  val hexUnmarshaller: Unmarshaller[String, ByteString] =
    Unmarshaller.strict(hex => ByteString.copyFrom(Base64.getDecoder.decode(hex)))

  val bigDecimalUnmarshaller: Unmarshaller[String, BigDecimal] =
    Unmarshaller.strict(s => BigDecimal(s))

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
        path("broadcast") {

          parameters(
            ('from.as(hexUnmarshaller), 'signature.as(hexUnmarshaller), 'fee.as(bigDecimalUnmarshaller), 'mode.?)) {
            (from, signature, fee, maybeMode) =>
              extractStrictEntity(1.second) { body =>
                val program = bodyToTransactionData(body)
                val tx = SignedTransaction(Address @@ from, program, signature, Mytc @@ fee, Random.nextInt)
                val mode = maybeMode.getOrElse("commit")
                val result = abciClient.broadcastTransaction(tx, mode)

                onSuccess(result) { stack =>
                  complete(utils.showStack(stack))
                }
              }
          }
        }
      }
    } ~ pathPrefix("private") {
      val paymentWallet = Config.timeChainConfig.paymentWallet
      post {
        path("broadcast") {

          parameters(('fee.as(bigDecimalUnmarshaller), 'mode.?)) { (fee, maybeMode) =>
            extractStrictEntity(1.second) { body =>
              val program = bodyToTransactionData(body)
              val from = paymentWallet.address
              val mode = maybeMode.getOrElse("commit")
              val result =
                abciClient.singAndBroadcastTransaction(from, paymentWallet.privateKey, program, Mytc @@ fee, mode)
              onSuccess(result) { stack =>
                complete(utils.showStack(stack))
              }
            }
          }
        }
      }
    }
}

object ApiRoute {}
