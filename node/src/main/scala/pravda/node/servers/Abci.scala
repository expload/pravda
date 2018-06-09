package pravda.node

package servers

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import com.tendermint.abci._
import pravda.node.db.{DB, Operation}
import pravda.vm.{Vm, state}
import pravda.vm.state.{Data, Environment, ProgramContext, Storage}
import pravda.node.clients.AbciClient
import pravda.node.data.blockchain.Transaction.AuthorizedTransaction
import pravda.node.data.common.{ApplicationStateInfo, TokenSaleMember, TransactionId}
import pravda.node.data.cryptography
import pravda.node.data.serialization._
import pravda.node.data.serialization.bson._
import pravda.node.data.serialization.json._
import pravda.node.persistence.FileStore
import pravda.common.contrib.ripemd160
import pravda.common.domain.{Address, NativeCoin}
import pravda.node.data.blockchain.Transaction.SignedTransaction

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import pravda.common.{bytes => byteUtils}
import pravda.node.data.blockchain.ExecutionInfo

class Abci(applicationStateDb: DB, abciClient: AbciClient, tokenSaleMembers: Seq[TokenSaleMember])(
    implicit ec: ExecutionContext)
    extends io.mytc.tendermint.abci.Api {

  import Abci._

  val consensusEnv = new EnvironmentProvider(applicationStateDb)
  val mempoolEnv = new EnvironmentProvider(applicationStateDb)

  var proposedHeight = 0L
  var validators: Vector[Address] = Vector.empty[Address]

  def info(request: RequestInfo): Future[ResponseInfo] = {
    FileStore.readApplicationStateInfoAsync().map { maybeInfo =>
      val info = maybeInfo.getOrElse(ApplicationStateInfo(0, ByteString.EMPTY, Vector.empty[Address]))
      ResponseInfo(lastBlockHeight = info.blockHeight, lastBlockAppHash = info.appHash)
    }
  }

  def initChain(request: RequestInitChain): Future[ResponseInitChain] = {

    val initValidators = request.validators.toVector
      .map(x => tendermint.unpackAddress(x.pubKey))

    for {
      _ <- FileStore
        .updateApplicationStateInfoAsync(ApplicationStateInfo(proposedHeight, ByteString.EMPTY, initValidators))
      _ <- Future.sequence(tokenSaleMembers.map {
        case TokenSaleMember(address, amount) =>
          applicationStateDb.putBytes(byteUtils.stringToBytes(s"balance:${byteUtils.byteString2hex(address)}"),
                                      transcode(amount).to[Bson])
      })
    } yield ResponseInitChain.defaultInstance

  }

  def beginBlock(request: RequestBeginBlock): Future[ResponseBeginBlock] = {
    consensusEnv.clear()

    val malicious = request.byzantineValidators.map(x => tendermint.unpackAddress(x.pubKey))
    val absent = request.absentValidators
    FileStore
      .readApplicationStateInfoAsync()
      .map { maybeInfo =>
        val info = maybeInfo.getOrElse(ApplicationStateInfo(0, ByteString.EMPTY, Vector.empty[Address]))
        validators = info.validators.zipWithIndex.collect {
          case (address, i) if !malicious.contains(address) && !absent.contains(i) => address
        }
      }
      .map(_ => ResponseBeginBlock())
  }

  def checkTransaction(tx: AuthorizedTransaction): Try[Unit] = {
    if (tx.wattPrice <= NativeCoin.zero) {
      Failure(WrongWattPriceException())
    } else if (tx.wattLimit <= 0) {
      Failure(WrongWattLimitException())
    } else {
      Success(())
    }
  }

  def deliverOrCheckTx[R](encodedTransaction: ByteString, environmentProvider: EnvironmentProvider)(
      result: (Int, String) => R): Future[R] = {

    val `try` = for {
      tx <- Try(transcode(Bson @@ encodedTransaction.toByteArray).to[SignedTransaction])
      authTx <- cryptography
        .checkTransactionSignature(tx)
        .fold[Try[AuthorizedTransaction]](Failure(TransactionUnauthorizedException()))(Success.apply)
      _ <- checkTransaction(authTx)
      tid = TransactionId.forEncodedTransaction(encodedTransaction)
      env = environmentProvider.transactionEnvironment(tid)
      _ <- Try(environmentProvider.withdraw(authTx.from, NativeCoin(authTx.wattPrice * authTx.wattLimit)))
      execResult = Vm.runRaw(authTx.program, authTx.from, env, authTx.wattLimit)
    } yield {
      val total = execResult.wattCounter.total
      val remaining = tx.wattLimit - total
      environmentProvider.accrue(tx.from, NativeCoin(tx.wattPrice * remaining))
      environmentProvider.appendFee(NativeCoin(authTx.wattPrice * total))
      if (execResult.isSuccess) {
        env.commitTransaction()
      }
      execResult
    }

    Future.successful {
      `try` match {
        case Success(executionResult) =>
          result(TxStatusOk, transcode(ExecutionInfo.from(executionResult)).to[Json])
        case Failure(e) =>
          val code =
            if (e.isInstanceOf[TransactionUnauthorizedException]) TxStatusUnauthorized
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
    // TODO: Validators update
    Future.successful(ResponseEndBlock.defaultInstance)
  }

  def commit(request: RequestCommit): Future[ResponseCommit] = {
    consensusEnv.commit(proposedHeight, validators)
    mempoolEnv.clear()
    val hash = ByteString.copyFrom(applicationStateDb.stateHash)
    FileStore
      .updateApplicationStateInfoAsync(ApplicationStateInfo(proposedHeight, hash, validators))
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

  final case class TransactionUnauthorizedException()   extends Exception("Transaction signature is invalid")
  final case class ProgramNotFoundException()           extends Exception("Program not found")
  final case class NotEnoughMoneyException()            extends Exception("Not enough money")
  final case class WrongWattPriceException()            extends Exception("Bad transaction parameter: wattPrice")
  final case class WrongWattLimitException()            extends Exception("Bad transaction parameter: wattLimit")
  final case class AmountShouldNotBeNegativeException() extends Exception("Amount should not be negative")

  final val TxStatusOk = 0
  final val TxStatusUnauthorized = 1
  final val TxStatusError = 2

  final case class StoredProgram(code: ByteString, owner: Address)

  sealed trait EnvironmentEffect

  object EnvironmentEffect {
    final case class StorageRemove(key: String, value: Option[Array[Byte]]) extends EnvironmentEffect
    final case class StorageWrite(key: String, previous: Option[Array[Byte]], value: Array[Byte])
        extends EnvironmentEffect
    final case class StorageRead(key: String, value: Option[Array[Byte]])  extends EnvironmentEffect
    final case class ProgramCreate(address: Address, program: Array[Byte]) extends EnvironmentEffect
    final case class ProgramUpdate(address: Address, program: Array[Byte]) extends EnvironmentEffect
    final case class Withdraw(from: Address, amount: NativeCoin)           extends EnvironmentEffect
    final case class Accrue(to: Address, amount: NativeCoin)               extends EnvironmentEffect
    final case class ShowBalance(address: Address, amount: NativeCoin)     extends EnvironmentEffect
  }

  final class EnvironmentProvider(db: DB) {

    private var fee = NativeCoin.zero
    private val operations = mutable.Buffer.empty[Operation]
    private val effectsMap = mutable.Buffer.empty[(TransactionId, mutable.Buffer[EnvironmentEffect])]
    private val cache = mutable.Map.empty[String, Option[Array[Byte]]]

    private lazy val programsPath = new CachedDbPath(new PureDbPath("program"), cache, operations)
    private lazy val effectsPath = new CachedDbPath(new PureDbPath("effects"), cache, operations)
    private lazy val balancesPath = new CachedDbPath(new PureDbPath("balance"), cache, operations)

    trait DbPath {

      def mkKey(suffix: String): String

      def :+(suffix: String): DbPath

      def getAs[V: BsonDecoder](suffix: String): Option[V] =
        getRawBytes(suffix).map(arr => transcode(Bson @@ arr).to[V])

      def getRawBytes(suffix: String): Option[Array[Byte]]

      def put[V: BsonEncoder](suffix: String, value: V): Option[Array[Byte]] = {
        val bsonValue: Array[Byte] = transcode(value).to[Bson]
        putRawBytes(suffix, bsonValue)
      }

      def putRawBytes(suffix: String, value: Array[Byte]): Option[Array[Byte]]

      def remove(suffix: String): Option[Array[Byte]]

      protected def returningPrevious(suffix: String)(f: => Unit): Option[Array[Byte]] = {
        val prev = getRawBytes(suffix)
        f
        prev
      }
    }

    private final class CachedDbPath(dbPath: DbPath,
                                     dbCache: mutable.Map[String, Option[Array[Byte]]],
                                     dbOperations: mutable.Buffer[Operation])
        extends DbPath {
      def mkKey(suffix: String): String = dbPath.mkKey(suffix)

      def :+(suffix: String) = new CachedDbPath(dbPath :+ suffix, dbCache, dbOperations)

      def getRawBytes(suffix: String): Option[Array[Byte]] = {
        val key = mkKey(suffix)
        dbCache.get(key).orElse(Option(dbPath.getRawBytes(suffix))).flatten
      }

      def putRawBytes(suffix: String, value: Array[Byte]): Option[Array[Byte]] = returningPrevious(suffix) {
        val key = mkKey(suffix)
        dbCache.put(key, Some(value))
        dbOperations += Operation.Put(byteUtils.stringToBytes(key), value)
      }

      def remove(suffix: String): Option[Array[Byte]] = returningPrevious(suffix) {
        val key = mkKey(suffix)
        dbCache.put(key, None)
      }

    }

    private final class PureDbPath(path: String) extends DbPath {

      def mkKey(suffix: String) = s"$path:$suffix"

      def :+(suffix: String) = new PureDbPath(mkKey(suffix))

      def getRawBytes(suffix: String): Option[Array[Byte]] = {
        val key = mkKey(suffix)
        db.syncGet(byteUtils.stringToBytes(key)).map(_.bytes)
      }

      def putRawBytes(suffix: String, value: Array[Byte]): Option[Array[Byte]] = returningPrevious(suffix) {
        val key = mkKey(suffix)
        db.syncPutBytes(byteUtils.stringToBytes(key), value)
      }

      def remove(suffix: String): Option[Array[Byte]] = returningPrevious(suffix) {
        val key = mkKey(suffix)
        db.syncDeleteBytes(byteUtils.stringToBytes(key))
      }

    }

    def transactionEnvironment(tid: TransactionId): TransactionDependentEnvironment = {
      val effects = mutable.Buffer.empty[EnvironmentEffect]
      effectsMap += (tid -> effects)
      new TransactionDependentEnvironment(effects)
    }

    final class TransactionDependentEnvironment(effects: mutable.Buffer[EnvironmentEffect]) extends Environment {

      private val transactionOperations = mutable.Buffer.empty[Operation]
      private val transactionEffects = mutable.Buffer.empty[EnvironmentEffect]
      private val transactionCache = mutable.Map.empty[String, Option[Array[Byte]]]

      private lazy val transactionProgramsPath = new CachedDbPath(programsPath, transactionCache, transactionOperations)
      private lazy val transactionBalancesPath = new CachedDbPath(balancesPath, transactionCache, transactionOperations)

      def commitTransaction(): Unit = {
        operations ++= transactionOperations
        cache ++= transactionCache
        effects ++= transactionEffects
      }

      import EnvironmentEffect._

      private final class WsProgramStorage(dbPath: DbPath) extends Storage {

        def get(key: state.Data): Option[state.Data] = {
          val hexKey = byteUtils.byteString2hex(key)
          val value = dbPath.getRawBytes(hexKey)
          transactionEffects += StorageRead(dbPath.mkKey(hexKey), value)
          value.map(ByteString.copyFrom)
        }

        def put(key: state.Data, value: state.Data): Option[state.Data] = {
          val hexKey = byteUtils.byteString2hex(key)
          val array = value.toByteArray
          val prev = dbPath.putRawBytes(byteUtils.byteString2hex(key), array)
          transactionEffects += StorageWrite(dbPath.mkKey(hexKey), prev, array)
          prev.map(ByteString.copyFrom)
        }

        def delete(key: state.Data): Option[state.Data] = {
          val hexKey = byteUtils.byteString2hex(key)
          val value = dbPath.remove(byteUtils.byteString2hex(key))
          transactionEffects += StorageRemove(dbPath.mkKey(hexKey), value)
          value.map(ByteString.copyFrom)
        }

      }

      def createProgram(owner: Address, code: Data): Address = {
        // FIXME fomkin: consider something better
        val addressBytes = ripemd160.getHash(owner.concat(code).toByteArray)

        val address = Address @@ ByteString.copyFrom(addressBytes)
        val sp = StoredProgram(code, owner)

        transactionProgramsPath.put(byteUtils.bytes2hex(addressBytes), sp)
        transactionEffects += ProgramCreate(address, code.toByteArray)
        address
      }

      def updateProgram(address: Address, code: Data): Data = {
        val oldSb = getStoredProgram(address).getOrElse(throw ProgramNotFoundException())
        val sp = oldSb.copy(code = code)
        transactionProgramsPath.put(byteUtils.byteString2hex(address), sp)
        transactionEffects += ProgramUpdate(address, code.toByteArray)
        sp.code
      }

      def transfer(from: Address, to: Address, amount: NativeCoin): Unit = {
        withdraw(from, amount)
        accrue(to, amount)
      }

      def balance(address: Address): NativeCoin = {
        val bal =
          transactionBalancesPath.getAs[NativeCoin](byteUtils.byteString2hex(address)).getOrElse(NativeCoin.zero)
        transactionEffects += ShowBalance(address, bal)
        bal
      }

      def withdraw(address: Address, amount: NativeCoin): Unit = {
        if (amount < 0) throw AmountShouldNotBeNegativeException()

        val current = balance(address)
        if (current < amount) throw NotEnoughMoneyException()

        transactionBalancesPath.put(byteUtils.byteString2hex(address), current - amount)
        transactionEffects += Withdraw(address, amount)

      }

      def accrue(address: Address, amount: NativeCoin): Unit = {
        if (amount < 0) throw AmountShouldNotBeNegativeException()

        val current = balance(address)
        transactionBalancesPath.put(byteUtils.byteString2hex(address), current + amount)
        transactionEffects += Accrue(address, amount)
      }

      private def getStoredProgram(address: ByteString) =
        transactionProgramsPath.getAs[StoredProgram](byteUtils.byteString2hex(address))

      // Effects below are ignored by effect collect
      // because they are inaccessible from user space

      def getProgramOwner(address: Address): Option[Address] =
        getStoredProgram(address).map(_.owner)

      def getProgram(address: Address): Option[ProgramContext] =
        getStoredProgram(address) map { program =>
          new ProgramContext {
            def code: ByteBuffer = ByteBuffer.wrap(program.code.toByteArray)
            def storage: Storage = {
              val newPath = transactionProgramsPath :+ byteUtils.byteString2hex(address)
              new WsProgramStorage(newPath)
            }
          }
        }

    }

    def appendFee(coins: NativeCoin): Unit = {
      val newFees = NativeCoin @@ (fee + coins)
      fee = newFees
    }

    def clear(): Unit = {
      operations.clear()
      effectsMap.clear()
      cache.clear()
      fee = NativeCoin.zero
    }

    def withdraw(address: Address, amount: NativeCoin): Unit = {
      if (amount < 0) throw AmountShouldNotBeNegativeException()
      val current = balancesPath.getAs[NativeCoin](byteUtils.byteString2hex(address)).getOrElse(NativeCoin.zero)
      if (current < amount) throw NotEnoughMoneyException()

      balancesPath.put(byteUtils.byteString2hex(address), current - amount)

    }

    def accrue(address: Address, amount: NativeCoin): Unit = {
      if (amount < 0) throw AmountShouldNotBeNegativeException()

      val current = balancesPath.getAs[NativeCoin](byteUtils.byteString2hex(address)).getOrElse(NativeCoin.zero)
      balancesPath.put(byteUtils.byteString2hex(address), current + amount)

    }

    def commit(height: Long, validators: Vector[Address]): Unit = {

      // Share fee
      val share = NativeCoin @@ (fee / validators.length)
      val remainder = NativeCoin @@ (fee % validators.length)
      validators.foreach { address =>
        accrue(address, share)
      }
      accrue(validators((height % validators.length).toInt), remainder)

      if (effectsMap.nonEmpty) {
        val data = effectsMap.toMap.asInstanceOf[Map[TransactionId, Seq[EnvironmentEffect]]]
        effectsPath.put(byteUtils.bytes2hex(byteUtils.longToBytes(height)), data)
      }

      db.syncBatch(operations: _*)
      clear()
    }

  }

}
