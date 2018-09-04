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
import pravda.common.domain._
import pravda.node
import pravda.node.clients.AbciClient
import pravda.node.data.blockchain.Transaction.SignedTransaction
import pravda.node.data.blockchain.{ExecutionInfo, TransactionData}
import pravda.node.data.common.TransactionId
import pravda.node.data.serialization.json._
import pravda.node.db.DB
import pravda.node.persistence.BlockChainStore._
import pravda.node.persistence.Entry
import pravda.node.servers.Abci.BlockDependentEnvironment
import pravda.vm.impl.{MemoryImpl, VmImpl, WattCounterImpl}
import pravda.vm.{Data, ExecutionResult}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, TimeoutException}
import scala.language.postfixOps
import scala.util.{Failure, Random, Success, Try}

class ApiRoute(abciClient: AbciClient, db: DB)(implicit executionContext: ExecutionContext) {

  val DryrunTimeout: FiniteDuration = 5 seconds

  import pravda.node.utils.AkkaHttpSpecials._

  val hexUnmarshaller: Unmarshaller[String, ByteString] =
    Unmarshaller.strict(hex => bytes.hex2byteString(hex))

  val bigDecimalUnmarshaller: Unmarshaller[String, BigDecimal] =
    Unmarshaller.strict(s => BigDecimal(s))

  val intUnmarshaller: Unmarshaller[String, Int] =
    Unmarshaller.strict(s => s.toInt)

  val balances: Entry[Address, NativeCoin] = balanceEntry(db)
  val events: Entry[ByteString, Data] = eventsEntry(db)

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

  def dryrun(from: Address, program: ByteString, currentBlockEnv: BlockDependentEnvironment): ExecutionResult = {
    val tid = TransactionId @@ ByteString.EMPTY
    val env = currentBlockEnv.transactionEnvironment(from, tid)
    val vm = new VmImpl()
    val execResultF: Future[ExecutionResult] = Future {
      vm.spawn(program, env, MemoryImpl.empty, new WattCounterImpl(Long.MaxValue), from)
    }
    Await.result(execResultF, DryrunTimeout) // FIXME: make it in non-blocking way
    // 5 seconds is very little amount of time
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
        post {
          withoutRequestTimeout {
            pathPrefix("broadcast") {
              path("dryRun") {
                parameters(
                  'from.as(hexUnmarshaller)
                ) { from =>
                  extractStrictEntity(1.second) { body =>
                    val program = bodyToTransactionData(body)
                    Try(dryrun(Address @@ from, program, new node.servers.Abci.BlockDependentEnvironment(db))) match {
                      case Success(result) => {
                        complete(ExecutionInfo.from(result))
                      }
                      case Failure(err: TimeoutException) => {
                        complete(ExecutionInfo(Some("Timeout"), 0, 0, 0, Nil, Nil))
                      }
                      case Failure(err) => {
                        complete(ExecutionInfo(Some(err.getMessage), 0, 0, 0, Nil, Nil))
                      }
                    }
                  }
                }
              }
            }
          }
        } ~
        get {
          path("balance") {
            parameters('address.as(hexUnmarshaller)) { (address) =>
              val f = balances
                .get(Address @@ address)
                .map(
                  _.getOrElse(NativeCoin @@ 0L)
                )
              onSuccess(f) { res =>
                complete(res)
              }
            }
          }
        } ~
        get {
          path("events") {
            parameters(
              (
                'address.as(hexUnmarshaller),
                'name,
                'offset.as(intUnmarshaller).?,
                'count.as(intUnmarshaller).?
              )) { (address, name, offsetO, countO) =>
              val offset = offsetO.getOrElse(0)
              val count = countO.map(c => math.max(c, ApiRoute.MaxEventCount)).getOrElse(ApiRoute.MaxEventCount)
              val f = events.startsWith(ByteString.copyFromUtf8(eventKey(Address @@ address, name)),
                                        ByteString.copyFromUtf8(eventKeyOffset(Address @@ address, name, offset.toLong)),
                                        count.toLong)
              onSuccess(f) { res =>
                complete(res.map(_.mkString()))
              }
            }
          }
        }
    }
}

object ApiRoute {
  final val MaxEventCount = 1000
}
