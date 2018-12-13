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

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}
import akka.http.scaladsl.server.{PathMatcher1, Route}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.google.protobuf.ByteString
import pravda.common.bytes
import pravda.common.domain._
import pravda.node
import pravda.node.clients.AbciClient
import pravda.node.clients.AbciClient.RpcError
import pravda.node.data.blockchain.Transaction.{SignedTransaction, UnsignedTransaction}
import pravda.node.data.blockchain.TransactionData
import pravda.node.data.common.TransactionId
import pravda.node.data.serialization._
import pravda.node.data.serialization.json._
import pravda.node.db.DB
import pravda.node.persistence.BlockChainStore._
import pravda.node.persistence.Entry
import pravda.node.servers.Abci.{TransactionEffects, TransactionResult}
import pravda.node.servers.ApiRoute.AddressPathMatcher
import pravda.vm.impl.VmImpl
import pravda.vm.{MarshalledData, ThrowableVmError}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success, Try}

/**
  * @param abci Direct access to transaction processing. Required by dry-run
  */
class ApiRoute(abciClient: AbciClient, db: DB, abci: Abci)(implicit executionContext: ExecutionContext) {

  import pravda.node.utils.AkkaHttpSpecials._
  import pravda.node.persistence.implicits._

  type R = Either[RpcError, TransactionResult]

  val hexUnmarshaller: Unmarshaller[String, ByteString] =
    Unmarshaller.strict(hex => bytes.hex2byteString(hex))

  val bigDecimalUnmarshaller: Unmarshaller[String, BigDecimal] =
    Unmarshaller.strict(s => BigDecimal(s))

  val intUnmarshaller: Unmarshaller[String, Int] =
    Unmarshaller.strict(s => s.toInt)

  val balances: Entry[Address, NativeCoin] = balanceEntry(db)
  val eventsByAddress: Entry[Address, Seq[TransactionEffects]] = eventsEntry(db)
  val transferEffectsByAddress: Entry[Address, Seq[TransactionEffects]] = transferEffectsEntry(db)
  val transactionsByAddress: Entry[Address, Seq[TransactionEffects]] = transactionsEntry(db)

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
        path("dryRun") {
          parameters(
            ('from.as(hexUnmarshaller),
             'signature.as(hexUnmarshaller).?,
             'nonce.as(intUnmarshaller).?,
             'wattLimit.as[Long],
             'wattPrice.as[Long],
             'wattPayer.as(hexUnmarshaller).?,
             'wattPayerSignature.as(hexUnmarshaller).?)) {
            (from, maybeSignature, maybeNonce, wattLimit, wattPrice, wattPayer, wattPayerSignature) =>
              extractStrictEntity(1.second) { body =>
                val env = new node.servers.Abci.BlockDependentEnvironment(db)
                val program = bodyToTransactionData(body)
                val nonce = maybeNonce.getOrElse(Random.nextInt())
                val verificationResult = maybeSignature match {
                  case Some(signature) =>
                    val tx = SignedTransaction(
                      Address @@ from,
                      program,
                      signature,
                      wattLimit,
                      NativeCoin @@ wattPrice,
                      wattPayer.map(Address @@ _),
                      wattPayerSignature,
                      nonce
                    )
                    abci.verifySignedTx(tx, TransactionId.Empty, env)
                  case None =>
                    val tx = UnsignedTransaction(
                      Address @@ from,
                      program,
                      wattLimit,
                      NativeCoin @@ wattPrice,
                      wattPayer.map(Address @@ _),
                      nonce
                    )
                    abci.verifyTx(tx, TransactionId.Empty, env)
                }

                type R = Either[RpcError, TransactionResult]

                verificationResult match {
                  case Success(x) =>
                    complete(Right(x): R)
                  case Failure(e: ThrowableVmError) =>
                    complete(Left(RpcError(-1, e.error.toString, "")): R)
                  case Failure(e: Abci.TransactionValidationException) =>
                    complete(Left(RpcError(-1, e.getMessage, "")): R)
                  case Failure(e) =>
                    failWith(e)
                }
              }
          }
        }
      } ~
        path("execute") {
          parameters('from.as(hexUnmarshaller)) { from =>
            extractStrictEntity(1.second) { body =>
              val bde = new node.servers.Abci.BlockDependentEnvironment(db)
              val program = bodyToTransactionData(body)
              val wattLimit = Long.MaxValue
              val transactionId = TransactionId.Empty
              val vm = new VmImpl()
              val env = bde.transactionEnvironment(Address @@ from, transactionId)

              val result = for {
                execResult <- Try(vm.spawn(program, env, wattLimit))
              } yield TransactionResult(transactionId, execResult, env.collectEffects)

              result match {
                case Success(x) =>
                  complete(Right(x): R)
                case Failure(e: ThrowableVmError) =>
                  complete(Left(RpcError(-1, e.error.toString, "")): R)
                case Failure(e: Abci.TransactionValidationException) =>
                  complete(Left(RpcError(-1, e.getMessage, "")): R)
                case Failure(e) =>
                  failWith(e)
              }
            }
          }
        } ~
        post {
          withoutRequestTimeout {
            path("broadcast") {
              parameters(
                ('from.as(hexUnmarshaller),
                 'signature.as(hexUnmarshaller),
                 'nonce.as(intUnmarshaller).?,
                 'wattLimit.as[Long],
                 'wattPrice.as[Long],
                 'wattPayer.as(hexUnmarshaller).?,
                 'wattPayerSignature.as(hexUnmarshaller).?,
                 'mode.?)) {
                (from, signature, maybeNonce, wattLimit, wattPrice, wattPayer, wattPayerSignature, maybeMode) =>
                  extractStrictEntity(1.second) { body =>
                    val program = bodyToTransactionData(body)
                    val nonce = maybeNonce.getOrElse(Random.nextInt())
                    val tx = SignedTransaction(
                      Address @@ from,
                      program,
                      signature,
                      wattLimit,
                      NativeCoin @@ wattPrice,
                      wattPayer.map(Address @@ _),
                      wattPayerSignature,
                      nonce
                    )

                    val mode = maybeMode.getOrElse("commit")

                    onSuccess(abciClient.broadcastTransaction(tx, mode)) { result =>
                      complete(result)
                    }
                  }
              }
            }
          }
        } ~
        get {
          path("balance") {
            parameters('address.as(hexUnmarshaller)) { address =>
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
                'program.as(hexUnmarshaller),
                'name,
                'transactionId.as(hexUnmarshaller).?,
                'offset.as(intUnmarshaller).?,
                'count.as(intUnmarshaller).?
              )) { (address, name, maybeTransaction, maybeOffset, maybeCount) =>
              val offset = maybeOffset.getOrElse(0)
              val count = maybeCount.fold(ApiRoute.MaxEventCount)(math.min(_, ApiRoute.MaxEventCount))
              val eventuallyResult = db
                .startsWith(
                  bytes.stringToBytes(s"events:${eventKey(Address @@ address, name)}"),
                  bytes.stringToBytes(s"events:${eventKeyOffset(Address @@ address, name, offset.toLong)}"),
                  count.toLong
                )
                .map { records =>
                  records.map(value =>
                    transcode(BJson @@ value.bytes)
                      .to[(TransactionId, MarshalledData)])
                }

              onSuccess(eventuallyResult) { result =>
                val items = {
                  val xs = result.zipWithIndex.map {
                    case ((tid, d), n) => ApiRoute.EventItem(n + offset, tid, d)
                  }
                  maybeTransaction.fold(xs)(tid => xs.filter(_.transactionId == tid))
                }
                complete(items)
              }
            }
          }
        } ~
        pathPrefix("account") {
          pathPrefix(AddressPathMatcher) { address =>
            path("transfers") {
              /*
               * Returns transfer effects from each transaction which affects to the
               * XCoins balance of the given address. In particular, will be returned
               * transfers FROM the given address and TO the given address.
               */
              get {
                onSuccess(transferEffectsByAddress.get(address)) { maybeEffectItems =>
                  complete(
                    maybeEffectItems.fold(Seq.empty[TransactionEffects])(identity)
                  )
                }
              }
            } ~
              path("events") {
                /*
                 * Returns all events from each transaction those were signed by the
                 * given address.
                 */
                get {
                  onSuccess(eventsByAddress.get(address)) { maybeEffectItems =>
                    complete(
                      maybeEffectItems.fold(Seq.empty[TransactionEffects])(identity)
                    )
                  }
                }
              } ~
              path("transactions") {
                /*
                 * Returns all transactions were signed by the given address.
                 */
                get {
                  onSuccess(transactionsByAddress.get(address)) { maybeTransactions =>
                    complete(
                      maybeTransactions.fold(Seq.empty[TransactionEffects])(identity)
                    )
                  }
                }
              }
          }
        }
    }
}

object ApiRoute {

  final val MaxEventCount = 1000

  object AddressPathMatcher extends PathMatcher1[Address] {

    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) ⇒
        Address.tryFromHex(segment).fold(_ => Unmatched, addr => Matched(tail, Tuple1(addr)))
      case _ ⇒ Unmatched
    }
  }

  final case class EventItem(offset: Int, transactionId: TransactionId, data: MarshalledData)
}
