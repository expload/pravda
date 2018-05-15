package io.mytc.timechain

package servers

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import com.tendermint.abci._
import io.mytc.keyvalue.{DB, Operation}
import io.mytc.sood.vm.{Vm, state}
import io.mytc.sood.vm.state.{Environment, ProgramContext, Storage}
import io.mytc.timechain.clients.AbciClient
import io.mytc.timechain.data.blockchain.Transaction
import io.mytc.timechain.data.blockchain.Transaction.AuthorizedTransaction
import io.mytc.timechain.data.common.{Address, ApplicationStateInfo}
import io.mytc.timechain.data.cryptography
import io.mytc.timechain.data.serialization._
import io.mytc.timechain.persistence.implicits._
import io.mytc.timechain.persistence.{Entry, FileStore}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class Abci(applicationStateDb: DB, abciClient: AbciClient)(implicit ec: ExecutionContext)
    extends io.mytc.tendermint.abci.Api {

  import Abci._

  var proposedHeight = 0L
  val mempoolEnv = new WorldStateEnvironment(applicationStateDb)
  val consensusEnv = new WorldStateEnvironment(applicationStateDb)

  def info(request: RequestInfo): Future[ResponseInfo] = {
    FileStore.readApplicationStateInfoAsync().map { maybeInfo =>
      val info = maybeInfo.getOrElse(ApplicationStateInfo(0, ByteString.EMPTY))
      ResponseInfo(lastBlockHeight = info.blockHeight, lastBlockAppHash = info.appHash)
    }
  }

  def initChain(request: RequestInitChain): Future[ResponseInitChain] = {
    Future.successful(ResponseInitChain.defaultInstance)
  }

  def beginBlock(request: RequestBeginBlock): Future[ResponseBeginBlock] = {
    consensusEnv.clear()
    Future.successful(ResponseBeginBlock())
  }

  def deliverTx(request: RequestDeliverTx): Future[ResponseDeliverTx] = {

    val `try` = for {
      tx <- Try(transcode(Bson @@ request.tx.toByteArray).to[Transaction.SignedTransaction])
      authTx <- cryptography
        .checkTransactionSignature(tx)
        .fold[Try[AuthorizedTransaction]](Failure(TransactionUnauthorized()))(Success.apply)
      _ <- Try(Vm.runRaw(authTx.program, authTx.from, consensusEnv))
    } yield {
      ()
    }

    Future.successful {
      `try` match {
        case Success(_) => ResponseDeliverTx(code = TxStatusOk)
        case Failure(e) =>
          ResponseDeliverTx(
            code =
              if (e.isInstanceOf[TransactionUnauthorized]) TxStatusUnauthorized
              else TxStatusError,
            log = e.getMessage
          )
      }
    }
  }

  def endBlock(request: RequestEndBlock): Future[ResponseEndBlock] = {
    proposedHeight = request.height
    Future.successful(ResponseEndBlock.defaultInstance)
  }

  def commit(request: RequestCommit): Future[ResponseCommit] = {
    consensusEnv.commit()
    mempoolEnv.clear()
    val hash = ByteString.copyFrom(applicationStateDb.stateHash)
    FileStore
      .updateApplicationStateInfoAsync(ApplicationStateInfo(proposedHeight, hash))
      .map(_ => ResponseCommit(hash))
  }

  def flush(request: RequestFlush): Future[ResponseFlush] = {
    // NOTE fomkin: it can be useful for implementing back pressure.
    Future.successful(ResponseFlush.defaultInstance)
  }

  def checkTx(request: RequestCheckTx): Future[ResponseCheckTx] = {
    val `try` = for {
      tx <- Try(transcode(Bson @@ request.tx.toByteArray).to[Transaction.SignedTransaction])
      authTx <- cryptography
        .checkTransactionSignature(tx)
        .fold[Try[AuthorizedTransaction]](Failure(TransactionUnauthorized()))(Success.apply)
      _ <- Try(Vm.runRaw(authTx.program, authTx.from, mempoolEnv))
    } yield {
      ()
    }

    Future.successful {
      `try` match {
        case Success(_) => ResponseCheckTx(code = TxStatusOk)
        case Failure(e) =>
          ResponseCheckTx(
            code =
              if (e.isInstanceOf[TransactionUnauthorized]) TxStatusUnauthorized
              else TxStatusError,
            log = e.getMessage
          )
      }
    }
  }

  def setOption(request: RequestSetOption): Future[ResponseSetOption] = ???

  def query(req: RequestQuery): Future[ResponseQuery] = ???
}

object Abci {

  final case class TransactionUnauthorized() extends Exception("Transaction signature is invalid")

  final val TxStatusOk = 0
  final val TxStatusUnauthorized = 1
  final val TxStatusError = 2

  final class WorldStateEnvironment(db: DB) extends Environment {

    private val operations = mutable.Buffer.empty[Operation]
    private val cache = mutable.Map.empty[ByteString, ByteString]
    private val programEntry = Entry[Address, ByteString](db, "program")

    private final class WsProgramStorage(entry: Entry[state.Address, state.Data]) extends Storage {

      def get(key: state.Address): Option[state.Data] =
        cache.get(key).orElse(entry.syncGet(key))

      def put(key: state.Address, value: state.Data): Unit = {
        cache.put(key, value)
        operations += Operation.Put(key, value)
      }

      def delete(key: state.Address): Unit = {
        cache.remove(key)
        operations += Operation.Delete(key)
      }
    }

    def getProgram(address: ByteString): Option[ProgramContext] =
      programEntry.syncGet(Address(address)) map { data =>
        new ProgramContext {
          def code: ByteBuffer = ByteBuffer.wrap(data.toByteArray)
          def storage: Storage = {
            val prefix = s"program:${utils.bytes2hex(address)}"
            val entry = Entry[ByteString, ByteString](db, prefix)
            new WsProgramStorage(entry)
          }
        }
      }

    def clear(): Unit = {
      operations.clear()
      cache.clear()
    }

    def commit(): Unit = {
      db.batch(operations: _*)
      clear()
    }
  }
}
