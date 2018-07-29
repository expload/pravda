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

import java.nio.ByteBuffer
import java.security.MessageDigest

import com.google.protobuf.ByteString
import com.tendermint.abci._
import pravda.node.clients.AbciClient
import pravda.node.data.blockchain.Transaction.{AuthorizedTransaction, SignedTransaction}
import pravda.node.data.common.{ApplicationStateInfo, CoinDistributionMember, TransactionId}
import pravda.node.data.cryptography
import pravda.node.data.serialization._
import pravda.node.data.serialization.bson._
import pravda.node.data.serialization.json._
import pravda.node.db.{DB, Operation}
import pravda.node.persistence.FileStore
import pravda.vm.impl.{MemoryImpl, VmImpl, WattCounterImpl}
import pravda.vm.{Environment, ProgramContext, Storage, _}
import pravda.node.persistence._
import pravda.common.domain.{Address, NativeCoin}
import pravda.node.data.blockchain.Transaction.SignedTransaction

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import pravda.common.{bytes => byteUtils}
import pravda.node.data.blockchain.ExecutionInfo
import pravda.node.persistence.BlockChainStore.balanceEntry

class Abci(applicationStateDb: DB, abciClient: AbciClient, initialDistribution: Seq[CoinDistributionMember])(
    implicit ec: ExecutionContext)
    extends io.mytc.tendermint.abci.Api {

  import Abci._

  val consensusEnv = new BlockDependentEnvironment(applicationStateDb)
  val mempoolEnv = new BlockDependentEnvironment(applicationStateDb)

  var proposedHeight = 0L
  var validators: Vector[Address] = Vector.empty[Address]
  val balances: Entry[Address, NativeCoin] = balanceEntry(applicationStateDb)

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
      _ <- Future.sequence(initialDistribution.map {
        case CoinDistributionMember(address, amount) =>
          balances.put(address, amount)
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

  def deliverOrCheckTx[R](encodedTransaction: ByteString, environmentProvider: BlockDependentEnvironment)(
      result: (Int, String) => R): Future[R] = {
    val `try` = for {
      tx <- Try(transcode(Bson @@ encodedTransaction.toByteArray).to[SignedTransaction])
      authTx <- cryptography
        .checkTransactionSignature(tx)
        .fold[Try[AuthorizedTransaction]](Failure(TransactionUnauthorizedException()))(Success.apply)
      _ <- checkTransaction(authTx)
      tid = TransactionId.forEncodedTransaction(encodedTransaction)
      env = environmentProvider.transactionEnvironment(authTx.from, tid)
      _ <- Try(environmentProvider.withdraw(authTx.from, NativeCoin(authTx.wattPrice * authTx.wattLimit)))
      vm = new VmImpl()
      execResult = vm.spawn(authTx.program, env, MemoryImpl.empty, new WattCounterImpl(authTx.wattLimit), authTx.from)
    } yield {
      if (execResult.isSuccess) {
        env.commitTransaction()
      }
      val total = execResult.wattCounter.total
      val remaining = authTx.wattLimit - total
      environmentProvider.accrue(authTx.from, NativeCoin(authTx.wattPrice * remaining))
      environmentProvider.appendFee(NativeCoin(authTx.wattPrice * total))
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
  final case class ProgramIsSealedException()           extends Exception("Program is sealed")
  final case class NotEnoughMoneyException()            extends Exception("Not enough money")
  final case class WrongWattPriceException()            extends Exception("Bad transaction parameter: wattPrice")
  final case class WrongWattLimitException()            extends Exception("Bad transaction parameter: wattLimit")
  final case class AmountShouldNotBeNegativeException() extends Exception("Amount should not be negative")

  final val TxStatusOk = 0
  final val TxStatusUnauthorized = 1
  final val TxStatusError = 2

  final case class StoredProgram(code: ByteString, owner: Address, `sealed`: Boolean)

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

  final class BlockDependentEnvironment(db: DB) {

    private var fee = NativeCoin.zero
    private val operations = mutable.Buffer.empty[Operation]
    private val effectsMap = mutable.Buffer.empty[(TransactionId, mutable.Buffer[EnvironmentEffect])]
    private val cache = mutable.Map.empty[String, Option[Array[Byte]]]

    private lazy val blockProgramsPath = new CachedDbPath(new PureDbPath(db, "program"), cache, operations)
    private lazy val blockEffectsPath = new CachedDbPath(new PureDbPath(db, "effects"), cache, operations)
    private lazy val blockBalancesPath = new CachedDbPath(new PureDbPath(db, "balance"), cache, operations)

    def transactionEnvironment(executor: Address, tid: TransactionId): TransactionDependentEnvironment = {
      val effects = mutable.Buffer.empty[EnvironmentEffect]
      effectsMap += (tid -> effects)
      new TransactionDependentEnvironment(executor, tid, effects)
    }

    final class TransactionDependentEnvironment(val executor: Address,
                                                transactionId: TransactionId,
                                                effects: mutable.Buffer[EnvironmentEffect])
        extends Environment {

      private val transactionOperations = mutable.Buffer.empty[Operation]
      private val transactionEffects = mutable.Buffer.empty[EnvironmentEffect]
      private val transactionCache = mutable.Map.empty[String, Option[Array[Byte]]]

      private lazy val transactionProgramsPath =
        new CachedDbPath(blockProgramsPath, transactionCache, transactionOperations)
      private lazy val transactionBalancesPath =
        new CachedDbPath(blockBalancesPath, transactionCache, transactionOperations)

      def commitTransaction(): Unit = {
        operations ++= transactionOperations
        cache ++= transactionCache
        effects ++= transactionEffects
      }

      import EnvironmentEffect._

      private final class WsProgramStorage(dbPath: DbPath) extends Storage {

        def get(key: Data): Option[Data] = {
          val hexKey = byteUtils.byteString2hex(key.toByteString)
          val value = dbPath.getRawBytes(hexKey)
          transactionEffects += StorageRead(dbPath.mkKey(hexKey), value)
          value.map(Data.fromBytes)
        }

        def put(key: Data, value: Data): Option[Data] = {
          val hexKey = byteUtils.byteString2hex(key.toByteString)
          val array = value.toByteString.toByteArray
          val prev = dbPath.putRawBytes(hexKey, array)
          transactionEffects += StorageWrite(dbPath.mkKey(hexKey), prev, array)
          prev.map(Data.fromBytes)
        }

        def delete(key: Data): Option[Data] = {
          val hexKey = byteUtils.byteString2hex(key.toByteString)
          val value = dbPath.remove(hexKey)
          transactionEffects += StorageRemove(dbPath.mkKey(hexKey), value)
          value.map(Data.fromBytes)
        }
      }

      def createProgram(owner: Address, code: ByteString, `sealed`: Boolean): Address = {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val addressBytes = sha256.digest(transactionId.concat(code).toByteArray)
        val address = Address @@ ByteString.copyFrom(addressBytes)
        val sp = StoredProgram(code, owner, `sealed`)

        transactionProgramsPath.put(byteUtils.bytes2hex(addressBytes), sp)
        transactionEffects += ProgramCreate(address, code.toByteArray)
        address
      }

      def updateProgram(address: Address, code: ByteString): Unit = {
        val oldSb = getStoredProgram(address).getOrElse(throw ProgramNotFoundException())
        if (oldSb.`sealed`) throw ProgramIsSealedException()
        val sp = oldSb.copy(code = code)
        transactionProgramsPath.put(byteUtils.byteString2hex(address), sp)
        transactionEffects += ProgramUpdate(address, code.toByteArray)
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
          val newPath = transactionProgramsPath :+ byteUtils.byteString2hex(address)
          val storage = new WsProgramStorage(newPath)
          ProgramContext(storage, ByteBuffer.wrap(program.code.toByteArray))
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
      val current = blockBalancesPath.getAs[NativeCoin](byteUtils.byteString2hex(address)).getOrElse(NativeCoin.zero)
      if (current < amount) throw NotEnoughMoneyException()

      blockBalancesPath.put(byteUtils.byteString2hex(address), current - amount)

    }

    def accrue(address: Address, amount: NativeCoin): Unit = {
      if (amount < 0) throw AmountShouldNotBeNegativeException()

      val current = blockBalancesPath.getAs[NativeCoin](byteUtils.byteString2hex(address)).getOrElse(NativeCoin.zero)
      blockBalancesPath.put(byteUtils.byteString2hex(address), current + amount)

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
        blockEffectsPath.put(byteUtils.bytes2hex(byteUtils.longToBytes(height)), data)
      }

      db.syncBatch(operations: _*)
      clear()
    }

  }

}
