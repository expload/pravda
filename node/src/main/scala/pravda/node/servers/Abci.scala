package pravda.node

package servers

import java.nio.ByteBuffer
import java.util.Base64

import com.google.protobuf.ByteString
import com.tendermint.abci._
import pravda.node.db.{DB, Operation}
import pravda.vm.{Vm, state}
import pravda.vm.state.{Data, Environment, ProgramContext, Storage}
import pravda.node.clients.AbciClient
import pravda.node.data.blockchain.Transaction.AuthorizedTransaction
import pravda.node.data.common.{ApplicationStateInfo, TransactionId}
import pravda.node.data.cryptography
import pravda.node.data.serialization._
import pravda.node.data.serialization.bson._
import pravda.node.persistence.FileStore
import pravda.common.contrib.ripemd160
import pravda.common.domain.{Address, NativeCoins}
import pravda.node.data.blockchain.Transaction.SignedTransaction

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import pravda.common.{bytes => byteUtils}

class Abci(applicationStateDb: DB, abciClient: AbciClient)(implicit ec: ExecutionContext)
    extends io.mytc.tendermint.abci.Api {

  import Abci._

  var proposedHeight = 0L
  val consensusEnv = new EnvironmentProvider(applicationStateDb)
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
      tx <- Try(transcode(Bson @@ encodedTransaction.toByteArray).to[SignedTransaction])
      authTx <- cryptography
        .checkTransactionSignature(tx)
        .fold[Try[AuthorizedTransaction]](Failure(TransactionUnauthorized()))(Success.apply)
      tid = TransactionId.forEncodedTransaction(encodedTransaction)
      env = environmentProvider.transactionEnvironment(tid)
      _ <- Try(env.withdraw(authTx.from, NativeCoins(authTx.wattPrice * authTx.wattLimit)))
      encodedStack <- Try {
        Vm.runRaw(authTx.program, authTx.from, env, authTx.wattLimit)
          .stack
          .map(bs => Base64.getEncoder.encodeToString(bs.toByteArray))
          .mkString(",")
      }
      // TODO: distribute coins
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
  final case class NotEnoughMoney() extends Exception("Not enough money")

  final val TxStatusOk = 0
  final val TxStatusUnauthorized = 1
  final val TxStatusError = 2

  final case class StoredProgram(code: ByteString, owner: Address)

  sealed trait EnvironmentEffect

  object EnvironmentEffect {
    final case class StorageRemove(key: String, value: Option[Array[Byte]]) extends EnvironmentEffect
    final case class StorageWrite(key: String, previous: Option[Array[Byte]], value: Array[Byte]) extends EnvironmentEffect
    final case class StorageRead(key: String, value: Option[Array[Byte]])   extends EnvironmentEffect
    final case class ProgramCreate(address: Address, program: Array[Byte])  extends EnvironmentEffect
    final case class ProgramUpdate(address: Address, program: Array[Byte])  extends EnvironmentEffect
    final case class Transfer(from: Address, to: Address, amount: NativeCoins) extends EnvironmentEffect
    final case class ShowBalance(address: Address, amount: NativeCoins) extends EnvironmentEffect
  }

  final class EnvironmentProvider(db: DB) {

    private val operations = mutable.Buffer.empty[Operation]
    private val effectsMap = mutable.Buffer.empty[(TransactionId, mutable.Buffer[EnvironmentEffect])]
    private val cache = mutable.Map.empty[String, Array[Byte]]

    private lazy val programsPath = new DbPath("program")
    private lazy val effectsPath = new DbPath("effects")
    private lazy val balancesPath = new DbPath("balance")

    private final class DbPath(path: String) {

      def mkKey(suffix: String) = s"$path:$suffix"

      def :+(suffix: String) = new DbPath(mkKey(suffix))

      def getAs[V: BsonDecoder](suffix: String): Option[V] =
        getRawBytes(suffix).map(arr => transcode(Bson @@ arr).to[V])

      def getRawBytes(suffix: String): Option[Array[Byte]] = {
        val key = mkKey(suffix)
        cache.get(key).orElse(db.syncGet(byteUtils.stringToBytes(key)).map(_.bytes))
      }

      def put[V: BsonEncoder](suffix: String, value: V): Option[Array[Byte]] = {
        val bsonValue: Array[Byte] = transcode(value).to[Bson]
        putRawBytes(suffix, bsonValue)
      }

      def putRawBytes(suffix: String, value: Array[Byte]): Option[Array[Byte]] = {
        val key = mkKey(suffix)
        val prev = getRawBytes(suffix)
        cache.put(key, value)
        operations += Operation.Put(byteUtils.stringToBytes(key), value)
        prev
      }

      def remove(suffix: String): Option[Array[Byte]] = {
        val key = mkKey(suffix)
        operations += Operation.Delete(byteUtils.stringToBytes(key))
        cache.remove(key).orElse(db.syncGet(byteUtils.stringToBytes(key)).map(_.bytes))
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

        def get(key: state.Data): Option[state.Data] = {
          val hexKey = byteUtils.byteString2hex(key)
          val value = dbPath.getRawBytes(hexKey)
          effects += StorageRead(dbPath.mkKey(hexKey), value)
          value.map(ByteString.copyFrom)
        }

        def put(key: state.Data, value: state.Data): Option[state.Data] = {
          val hexKey = byteUtils.byteString2hex(key)
          val array = value.toByteArray
          val prev = dbPath.putRawBytes(byteUtils.byteString2hex(key), array)
          effects += StorageWrite(dbPath.mkKey(hexKey), prev, array)
          prev.map(ByteString.copyFrom)
        }

        def delete(key: state.Data): Option[state.Data] = {
          val hexKey = byteUtils.byteString2hex(key)
          val value = dbPath.remove(byteUtils.byteString2hex(key))
          effects += StorageRemove(dbPath.mkKey(hexKey), value)
          value.map(ByteString.copyFrom)
        }

      }

      def createProgram(owner: Address, code: Data): Address = {
        // FIXME fomkin: consider something better
        val addressBytes = ripemd160.getHash(owner.concat(code).toByteArray)

        val address = Address @@ ByteString.copyFrom(addressBytes)
        val sp = StoredProgram(code, owner)

        programsPath.put(byteUtils.bytes2hex(addressBytes), sp)

        effects += ProgramCreate(address, code.toByteArray)
        address
      }

      def updateProgram(address: Address, code: Data): Data = {
        val oldSb = getStoredProgram(address).getOrElse(throw ProgramNotFoundException())
        val sp = oldSb.copy(code = code)
        programsPath.put(byteUtils.byteString2hex(address), sp)
        effects += ProgramUpdate(address, code.toByteArray)
        sp.code
      }

      def transfer(from: Address, to: Address, amount: NativeCoins): Unit = {
        withdraw(from, amount)
        put(to, amount)
        effects += Transfer(from, to, amount)
      }

      def balance(address: Address): NativeCoins = {
        val bal = balancesPath.getAs[NativeCoins](byteUtils.byteString2hex(address)).getOrElse(NativeCoins.zero)
        effects += ShowBalance(address, bal)
        bal
      }

      def withdraw(address: Address, amount: NativeCoins): Unit = {
        val current = balance(address)
        if(current < amount) {
          throw NotEnoughMoney()
        } else {
          balancesPath.put(byteUtils.byteString2hex(address), current - amount)
        }
      }

      def put(address: Address, amount: NativeCoins): Unit = {
        val current = balance(address)
        balancesPath.put(byteUtils.byteString2hex(address), current + amount)
      }

      private def getStoredProgram(address: ByteString) =
        programsPath.getAs[StoredProgram](byteUtils.byteString2hex(address))

      // Effects below are ignored by effect collect
      // because they are inaccessible from user space

      def getProgramOwner(address: Address): Option[Address] =
        getStoredProgram(address).map(_.owner)

      def getProgram(address: Address): Option[ProgramContext] =
        getStoredProgram(address) map { program =>
          new ProgramContext {
            def code: ByteBuffer = ByteBuffer.wrap(program.code.toByteArray)
            def storage: Storage = {
              val newPath = programsPath :+ byteUtils.byteString2hex(address)
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

      if (effectsMap.nonEmpty) {
        val data = effectsMap.toMap.asInstanceOf[Map[TransactionId, Seq[EnvironmentEffect]]]
        effectsPath.put(byteUtils.bytes2hex(byteUtils.longToBytes(height)), data)
      }

      db.syncBatch(operations: _*)
      clear()
    }
  }
}
