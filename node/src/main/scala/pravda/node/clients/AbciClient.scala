package pravda.node.clients

import java.util.Base64

import com.google.protobuf.{ByteString => PbByteString}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import pravda.node.data.blockchain.Transaction.SignedTransaction
import pravda.node.data.blockchain.{Transaction, TransactionData}
import pravda.node.data.common.TransactionId
import pravda.node.data.cryptography
import pravda.node.data.cryptography.PrivateKey
import pravda.node.data.serialization._
import pravda.node.data.serialization.bson._
import pravda.node.data.serialization.json._
import pravda.common.bytes._
import pravda.common.domain.{Address, NativeCoin}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Random

class AbciClient(port: Int)(implicit
                            system: ActorSystem,
                            materializer: ActorMaterializer,
                            executionContext: ExecutionContextExecutor) {

  // Response format:
  // https://tendermint.readthedocs.io/en/master/getting-started.html

  import AbciClient._

//  private def throwIfError(prefix: String, res: TxResult): Unit = {
//    if (res.code != 0)
//      throw new RuntimeException(s"${prefix} error: ${res.code}:${res.log}")
//  }

  type ErrorOrStack = Either[RpcError, List[PbByteString]]

  private def handleResponse(response: HttpResponse, mode: String): Future[ErrorOrStack] = {
    response match {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map { body =>
          val json = body.utf8String
          mode match {
            case "commit" =>
              val response = transcode(Json @@ json).to[RpcCommitResponse]
              response.result
                .map { result =>
                  Right(
                    result.deliver_tx.log
                      .split(',')
                      .toList
                      .map(s => PbByteString.copyFrom(Base64.getDecoder.decode(s)))
                  )
                }
                .orElse(response.error.map(Left.apply))
                .getOrElse(Left(UnknownError))
            case _ =>
              Left(UnknownError)
          }
        }
      case HttpResponse(code, _, _, _) =>
        response.discardEntityBytes()
        Future.failed(RpcHttpException(code.intValue()))
    }
  }

  def readTransaction(id: TransactionId): Future[SignedTransaction] = {

    val uri = Uri(s"http://127.0.0.1:$port/tx")
      .withQuery(Uri.Query("hash" -> ("0x" + byteString2hex(id))))

    Http()
      .singleRequest(HttpRequest(uri = uri))
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map { data =>
            val txResponse = transcode(Json @@ data.utf8String).to[AbciClient.RpcTxResponse]
            txResponse.error match {
              case Some(error) =>
                throw RpcException(error)
              case None =>
                transcode(Bson @@ txResponse.result.get.tx.toByteArray).to[SignedTransaction]
            }
          }
        case HttpResponse(code, _, entity, _) =>
          entity.discardBytes()
          throw RpcHttpException(code.intValue())
      }
  }

  def broadcastBytes(bytes: Array[Byte], mode: String = "commit"): Future[ErrorOrStack] = {

    val uri = Uri(s"http://127.0.0.1:$port/broadcast_tx_$mode")
      .withQuery(Uri.Query("tx" -> ("0x" + bytes2hex(bytes))))

    Http()
      .singleRequest(HttpRequest(uri = uri))
      .flatMap(handleResponse(_, mode))
  }

  def broadcastTransaction(tx: SignedTransaction, mode: String = "commit"): Future[ErrorOrStack] = {

    val bytes = transcode(tx).to[Bson]
    broadcastBytes(bytes, mode)
  }

  def singAndBroadcastTransaction(from: Address,
                                  privateKey: PrivateKey,
                                  data: TransactionData,
                                  wattLimit: Long,
                                  wattPrice: BigDecimal,
                                  mode: String = "commit"): Future[ErrorOrStack] = {

    val unsignedTx = Transaction.UnsignedTransaction(from, data, wattLimit, wattPrice, Random.nextInt())
    val tx = cryptography.signTransaction(privateKey, unsignedTx)
    val bytes = transcode(tx).to[Bson]
    broadcastBytes(bytes, mode)
  }
}

object AbciClient {

  final val UnknownError = RpcError(-1, "", "")

  final case class RpcException(error: RpcError) extends Exception(s"${error.message}: ${error.data}")
  final case class RpcHttpException(httpCode: Int)
      extends Exception(s"RPC request to Tendermint failed with HTTP code $httpCode")

  final case class TxSyncResult(check_tx: TxResult)
  final case class RpcSyncResponse(jsonrpc: String, id: String, result: TxSyncResult)
  final case class RpcAsyncResponse(jsonrpc: String, id: String, result: TxResult)

  final case class RpcCommitResponse(result: Option[TxCommitResult], error: Option[RpcError])
  final case class TxCommitResult(check_tx: TxResult, deliver_tx: TxResult)
  final case class TxResult(log: String)

  final case class RpcError(code: Int, message: String, data: String)
  final case class RpcTxResponse(error: Option[RpcError], result: Option[RpcTxResponse.Result])

  object RpcTxResponse {
    final case class Result(height: Long, tx: PbByteString)
  }
}
