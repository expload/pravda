package io.mytc.timechain.clients

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import io.mytc.timechain.data.blockchain.{Transaction, TransactionData}
import io.mytc.timechain.data.common.{Address, Mytc}
import io.mytc.timechain.data.cryptography
import io.mytc.timechain.data.cryptography.PrivateKey
import io.mytc.timechain.data.serialization._
import io.mytc.timechain.data.serialization.boopick._
import io.mytc.timechain.data.serialization.json._
import io.mytc.timechain.utils.bytes2hex

import scala.concurrent.{ExecutionContextExecutor, Future}

class AbciClient(port: Int)(implicit
                            system: ActorSystem,
                            materializer: ActorMaterializer,
                            executionContext: ExecutionContextExecutor) {

  // Response format:
  // https://tendermint.readthedocs.io/en/master/getting-started.html

  import AbciClient._

  private def throwIfError(prefix: String, res: TxResult): Unit = {
    if (res.code != 0)
      throw new RuntimeException(s"${prefix} error: ${res.code}:${res.log}")
  }

  private def handleResponse(response: HttpResponse, mode: String): Future[Unit] = Future {
    response match {
      case HttpResponse(StatusCodes.OK, _, entity, _) => {
        entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
          val jsonBody = body.utf8String
          mode match {
            case "async" ⇒
              val res = transcode(Json @@ jsonBody).to[RpcAsyncResponse].result
              throwIfError("async request", res)
            case "sync" ⇒
              val res = transcode(Json @@ jsonBody).to[RpcSyncResponse].result
              throwIfError("check tx", res.check_tx)
            case "commit" ⇒
              val res = transcode(Json @@ jsonBody).to[RpcCommitResponse].result
              throwIfError("check tx", res.check_tx)
              throwIfError("deliver tx", res.deliver_tx)
            case _ ⇒ ()
          }
        }
      }
      case HttpResponse(code, _, _, _) => {
        response.discardEntityBytes()
        System.err.println(s"tx broadcast request error: http code ${code}")
      }
    }
  }

  def broadcastTransaction(from: Address,
                           privateKey: PrivateKey,
                           data: TransactionData,
                           fee: Mytc,
                           mode: String = "commit"): Future[Unit] = {

    val unsignedTx = Transaction.UnsignedTransaction(from, data, fee)
    val tx = cryptography.signTransaction(privateKey, unsignedTx)
    val bytes = transcode(tx).to[BooPickle]

    val uri = Uri(s"http://127.0.0.1:${port}/broadcast_tx_$mode")
      .withQuery(Uri.Query("tx" -> ("0x" + bytes2hex(bytes))))

    Http()
      .singleRequest(HttpRequest(uri = uri))
      .flatMap(handleResponse(_, mode))
  }
}

object AbciClient {
  final case class TxResult(code: Int, data: String, log: String)
  final case class TxSyncResult(check_tx: TxResult)
  final case class TxCommitResult(check_tx: TxResult, deliver_tx: TxResult)
  final case class RpcSyncResponse(jsonrpc: String, id: String, result: TxSyncResult)
  final case class RpcAsyncResponse(jsonrpc: String, id: String, result: TxResult)
  final case class RpcCommitResponse(jsonrpc: String, id: String, result: TxCommitResult)
}
