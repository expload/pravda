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
import pravda.common.domain.{Address, NativeCoin}
import pravda.node.clients.AbciClient
import pravda.node.data.blockchain.Transaction.SignedTransaction
import pravda.node.data.blockchain.TransactionData
import pravda.node.data.serialization.json._
import pravda.node.db.DB
import pravda.node.persistence.BlockChainStore.balanceEntry
import pravda.node.persistence.Entry

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

class ApiRoute(abciClient: AbciClient, db: DB)(implicit executionContext: ExecutionContext) {

  import pravda.node.utils.AkkaHttpSpecials._

  val hexUnmarshaller: Unmarshaller[String, ByteString] =
    Unmarshaller.strict(hex => bytes.hex2byteString(hex))

  val bigDecimalUnmarshaller: Unmarshaller[String, BigDecimal] =
    Unmarshaller.strict(s => BigDecimal(s))

  val intUnmarshaller: Unmarshaller[String, Int] =
    Unmarshaller.strict(s => s.toInt)

  val balances: Entry[Address, NativeCoin] = balanceEntry(db)

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
               'wattLimit.as[Long],
               'wattPrice.as[Long],
               'mode.?)) { (from, signature, maybeNonce, wattLimit, wattPrice, maybeMode) =>
              extractStrictEntity(1.second) { body =>
                val program = bodyToTransactionData(body)
                val nonce = maybeNonce.getOrElse(Random.nextInt())
                val tx =
                  SignedTransaction(Address @@ from, program, signature, wattLimit, NativeCoin @@ wattPrice, nonce)
                println(Show[SignedTransaction].show(tx))
                val mode = maybeMode.getOrElse("commit")
                val result = abciClient.broadcastTransaction(tx, mode)

                onSuccess(result) {
                  case Right(info) => complete(info)
                  case Left(error) => complete(error)
                }
              }
            }
          }
        }
      } ~
      get {
        path("balance") {
          parameters('address.as(hexUnmarshaller)) {
            (address) =>
              val f = balances.get(Address @@ address).map(
                _.getOrElse(NativeCoin @@ 0L)
              )
              onSuccess(f) {
                res => complete(res)
              }
          }
        }
      }
    }
}

object ApiRoute {}
