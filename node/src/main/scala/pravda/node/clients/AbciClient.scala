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

package pravda.node.clients

import com.google.protobuf.{ByteString => PbByteString}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import pravda.common.data.blockchain.Transaction.SignedTransaction
import pravda.common.data.blockchain.{Transaction, TransactionData}
import pravda.common.data.blockchain.TransactionId
import pravda.common.serialization._
import pravda.node.data.serialization._
import pravda.common.serialization.protobuf._
import pravda.node.data.serialization.json._
import pravda.common.bytes._
import pravda.common.cryptography
import pravda.common.domain.{Address, NativeCoin, PrivateKey}
import pravda.node.servers.Abci.TransactionResult

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

  type ErrorOrExecInfo = Either[RpcError, TransactionResult]

  private def handleResponse(response: HttpResponse, mode: String): Future[ErrorOrExecInfo] = {
    response match {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map { body =>
          val json = body.utf8String
          mode match {
            case "commit" =>
              val response = transcode(Json @@ json).to[RpcCommitResponse]
              response.result
                .map { result =>
                  result.deliver_tx.log match {
                    case Some(log) => Right(transcode(Json @@ log).to[TransactionResult])
                    case None      => Left(result.check_tx.log.map(s => RpcError(-1, s, "")).getOrElse(UnknownError))
                  }
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
                transcode(Protobuf @@ txResponse.result.get.tx.toByteArray).to[SignedTransaction]
            }
          }
        case HttpResponse(code, _, entity, _) =>
          entity.discardBytes()
          throw RpcHttpException(code.intValue())
      }
  }

  def broadcastBytes(bytes: Array[Byte], mode: String = "commit"): Future[ErrorOrExecInfo] = {

    val uri = Uri(s"http://127.0.0.1:$port/broadcast_tx_$mode")
      .withQuery(Uri.Query("tx" -> ("0x" + bytes2hex(bytes))))

    Http()
      .singleRequest(HttpRequest(uri = uri))
      .flatMap(handleResponse(_, mode))
  }

  def broadcastTransaction(tx: SignedTransaction, mode: String = "commit"): Future[ErrorOrExecInfo] = {

    val bytes = transcode(tx).to[Protobuf]
    broadcastBytes(bytes, mode)
  }

  def singAndBroadcastTransaction(from: Address,
                                  privateKey: PrivateKey,
                                  data: TransactionData,
                                  wattLimit: Long,
                                  wattPrice: NativeCoin,
                                  mode: String = "commit"): Future[ErrorOrExecInfo] = {

    val unsignedTx = Transaction.UnsignedTransaction(from, data, wattLimit, wattPrice, None, Random.nextInt())
    val tx = cryptography.signTransaction(privateKey, unsignedTx)
    val bytes = transcode(tx).to[Protobuf]
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
  final case class TxResult(log: Option[String])

  final case class RpcError(code: Int, message: String, data: String)
  final case class RpcTxResponse(error: Option[RpcError], result: Option[RpcTxResponse.Result])

  object RpcTxResponse {
    final case class Result(hash: String, height: String, tx: PbByteString)
  }
}
