package io.mytc.timechain

package servers

import java.nio.ByteBuffer
import java.util.Base64

import com.google.protobuf.ByteString
import com.tendermint.abci._
import io.mytc.keyvalue.serialyzer.ValueWriter
import io.mytc.keyvalue.{DB, Operation}
import io.mytc.sood.vm.{Vm, state}
import io.mytc.sood.vm.state.{Data, Environment, ProgramContext, Storage}
import io.mytc.timechain.clients.AbciClient
import io.mytc.timechain.data.blockchain.Transaction
import io.mytc.timechain.data.blockchain.Transaction.AuthorizedTransaction
import io.mytc.timechain.data.common.{Address, ApplicationStateInfo, TransactionId}
import io.mytc.timechain.data.cryptography
import io.mytc.timechain.data.serialization._
import io.mytc.timechain.persistence.implicits._
import io.mytc.timechain.persistence.FileStore
import io.mytc.timechain.contrib.ripemd160

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class Abci(applicationStateDb: DB, abciClient: AbciClient)(implicit ec: ExecutionContext)
    extends io.mytc.tendermint.abci.Api {

  import Abci._

  var proposedHeight = 0L
  val consensusEnv = new EnvironmentProvider(applicationStateDb)
  // FIXME fomkin: mempool should work without data base
  val mempoolEnv = new EnvironmentProvider(applicationStateDb)

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

  def deliverOrCheckTx[R](encodedTransaction: ByteString, environmentProvider: EnvironmentProvider)(
      result: (Int, String) => R): Future[R] = {
    val `try` = for {
      tx <- Try(transcode(Bson @@ encodedTransaction.toByteArray).to[Transaction.SignedTransaction])
      authTx <- cryptography
        .checkTransactionSignature(tx)
        .fold[Try[AuthorizedTransaction]](Failure(TransactionUnauthorized()))(Success.apply)
      tid = TransactionId.forEncodedTransaction(encodedTransaction)
      env = environmentProvider.transactionEnvironment(tid)
      encodedStack <- Try {
        Vm.runRaw(authTx.program, authTx.from, env)
          .stack
          .map(bs => Base64.getEncoder.encodeToString(bs.toByteArray))
          .mkString(",")
      }
    } yield {
      encodedStack
    }

    Future.successful {
      `try` match {
        case Success(encodedStack) =>
          result(TxStatusOk, encodedStack)
        case Failure(e) =>
          val code =
            if (e.isInstanceOf[TransactionUnauthorized]) TxStatusUnauthorized
            else TxStatusError
          result(code, e.getMessage)
      }
    }
  }

  def deliverTx(request: RequestDeliverTx): Future[ResponseDeliverTx] = {
    deliverOrCheckTx(request.tx, consensusEnv) { (code, log) =>
      ResponseDeliverTx(code = code, log = log)
    }
  }

  def endBlock(request: RequestEndBlock): Future[ResponseEndBlock] = {
    proposedHeight = request.height
    Future.successful(ResponseEndBlock.defaultInstance)
  }

  def commit(request: RequestCommit): Future[ResponseCommit] = {
    consensusEnv.commit(proposedHeight)
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
    deliverOrCheckTx(request.tx, mempoolEnv) { (code, log) =>
      ResponseCheckTx(code = code, log = log)
    }
  }

  def setOption(request: RequestSetOption): Future[ResponseSetOption] = ???

  def query(req: RequestQuery): Future[ResponseQuery] = ???
}

object Abci {

  final case class TransactionUnauthorized()  extends Exception("Transaction signature is invalid")
  final case class ProgramNotFoundException() extends Exception("Program not found")

  final val TxStatusOk = 0
  final val TxStatusUnauthorized = 1
  final val TxStatusError = 2

  final case class StoredProgram(code: ByteString, owner: Address)

  sealed trait EnvironmentEffect

  object EnvironmentEffect {
    final case class StorageRemove(key: String, value: Option[Array[Byte]]) extends EnvironmentEffect
    final case class StorageWrite(key: String, value: Array[Byte])          extends EnvironmentEffect
    final case class StorageRead(key: String, value: Option[Array[Byte]])   extends EnvironmentEffect
    final case class ProgramCreate(address: Address, program: Array[Byte])  extends EnvironmentEffect
    final case class ProgramUpdate(address: Address, program: Array[Byte])  extends EnvironmentEffect
  }

  final class EnvironmentProvider(db: DB) {

    val dataWriter: ValueWriter[Data] = (value: Data) => value.toByteArray

    private val operations = mutable.Buffer.empty[Operation]
    private val effectsMap = mutable.Buffer.empty[(TransactionId, mutable.Buffer[EnvironmentEffect])]
    private val cache = mutable.Map.empty[String, Array[Byte]]

    private lazy val programsPath = new DbPath("program")
    private lazy val effectsPath = new DbPath("effects")

    private final class DbPath(path: String) {

      def mkKey(suffix: String) = s"$path:$suffix"

      def :+(suffix: String) = new DbPath(mkKey(suffix))

      def get(suffix: String): Option[Array[Byte]] = {
        val key = mkKey(suffix)
        cache.get(key).orElse(db.syncGet(key).map(_.bytes))
      }

      def put[V](suffix: String, value: V)(implicit vw: ValueWriter[V]): Unit = {
        val key = mkKey(suffix)
        cache.put(key, vw.toBytes(value))
        operations += Operation.Put(key, value)
      }

      def remove(suffix: String): Option[Array[Byte]] = {
        val key = mkKey(suffix)
        operations += Operation.Delete(key)
        cache.remove(key)
      }
    }

    def transactionEnvironment(tid: TransactionId): Environment = {
      val effects = mutable.Buffer.empty[EnvironmentEffect]
      effectsMap += (tid -> effects)
      new TransactionDependentEnvironment(effects)
    }

    private final class TransactionDependentEnvironment(effects: mutable.Buffer[EnvironmentEffect])
        extends Environment {

      import EnvironmentEffect._

      private final class WsProgramStorage(dbPath: DbPath) extends Storage {

        def get(key: state.Address): Option[state.Data] = {
          val hexKey = utils.bytes2hex(key)
          val value = dbPath.get(hexKey)
          effects += StorageRead(dbPath.mkKey(hexKey), value)
          value.map(ba => ByteString.copyFrom(ba))
        }

        def put(key: state.Address, value: state.Data): Unit = {
          val hexKey = utils.bytes2hex(key)
          effects += StorageWrite(dbPath.mkKey(hexKey), value.toByteArray)
          dbPath.put(utils.bytes2hex(key), value)(dataWriter) // FIXME remove explicit writer
        }

        def delete(key: state.Address): Unit = {
          val hexKey = utils.bytes2hex(key)
          val value = dbPath.remove(utils.bytes2hex(key))
          effects += StorageRemove(dbPath.mkKey(hexKey), value)
        }
      }

      def createProgram(owner: state.Address, code: Data): state.Address = {
        // FIXME fomkin: consider something better
        val addressBytes = ripemd160.getHash(owner.concat(code))

        val address = Address @@ ByteString.copyFrom(addressBytes)
        val sp = StoredProgram(code, Address @@ owner)

        programsPath.put(utils.bytes2hex(addressBytes), sp)

        effects += ProgramCreate(address, code.toByteArray)
        address
      }

      def updateProgram(address: state.Address, code: Data): Unit = {
        val oldSb = getStoredProgram(address).getOrElse(throw ProgramNotFoundException())
        val sp = oldSb.copy(code = code)
        programsPath.put(utils.bytes2hex(address), sp)
        effects += ProgramUpdate(Address @@ address, code.toByteArray)
      }

      private def getStoredProgram(address: ByteString) =
        programsPath.get(utils.bytes2hex(address)) map { serializedProgram =>
          transcode(Bson @@ serializedProgram).to[StoredProgram]
        }

      // Effects below are ignored by effect collect
      // because they are inaccessible from user space

      def getProgramOwner(address: ByteString): Option[ByteString] =
        getStoredProgram(address).map(_.owner)

      def getProgram(address: ByteString): Option[ProgramContext] =
        getStoredProgram(address) map { program =>
          new ProgramContext {
            def code: ByteBuffer = ByteBuffer.wrap(program.code.toByteArray)
            def storage: Storage = {
              val newPath = programsPath :+ utils.bytes2hex(address)
              new WsProgramStorage(newPath)
            }
          }
        }
    }

    def clear(): Unit = {
      operations.clear()
      effectsMap.clear()
      cache.clear()
    }

    def commit(height: Long): Unit = {
      import utils.padLong
      if (effectsMap.nonEmpty) {
        val data = effectsMap.toMap.asInstanceOf[Map[TransactionId, Seq[EnvironmentEffect]]]
        effectsPath.put(padLong(height, 10), data)
      }

      db.syncBatch(operations: _*)
      clear()
    }
  }
}
