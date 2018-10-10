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
import pravda.common.domain._
import pravda.common.{bytes => byteUtils}
import pravda.node.clients.AbciClient
import pravda.node.data.blockchain.Transaction
import pravda.node.data.blockchain.Transaction.{AuthorizedTransaction, SignedTransaction}
import pravda.node.data.common.{ApplicationStateInfo, CoinDistributionMember, TransactionId}
import pravda.node.data.cryptography
import pravda.node.data.serialization._
import pravda.node.data.serialization.bson._
import pravda.node.data.serialization.json._
import pravda.node.db.{DB, Operation}
import pravda.node.persistence.BlockChainStore.balanceEntry
import pravda.node.persistence.{FileStore, _}
import pravda.vm
import pravda.vm.impl.VmImpl
import pravda.vm.{Environment, ProgramContext, Storage, _}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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

  private def checkTxWatts(tx: Transaction): Try[Unit] = {
    if (tx.wattPrice <= NativeCoin.zero) {
      Failure(WrongWattPriceException())
    } else if (tx.wattLimit <= 0) {
      Failure(WrongWattLimitException())
    } else {
      Success(())
    }
  }

  def verifyTx(tx: Transaction, id: TransactionId, ep: BlockDependentEnvironment): Try[TransactionResult] = {

    val vm = new VmImpl()
    val env = ep.transactionEnvironment(tx.from, id)
    val wattPayer = tx.wattPayer.fold(tx.from)(identity)

    for {
      _ <- checkTxWatts(tx)
      // Select watt payer. if watt payer is defined use them,
      // else use transaction sender (from).
      // Signature is checked above in `checkTransaction` function.
      _ <- Try(ep.withdraw(wattPayer, NativeCoin(tx.wattPrice * tx.wattLimit)))
      execResult <- Try(vm.spawn(tx.program, env, tx.wattLimit))
    } yield {
      val total = execResult match {
        case Left(RuntimeException(_, state, _, _)) => state.totalWatts
        case Right(state) =>
          env.commitTransaction()
          state.totalWatts
      }
      val remaining = tx.wattLimit - total
      ep.accrue(wattPayer, NativeCoin(tx.wattPrice * remaining))
      ep.appendFee(NativeCoin(tx.wattPrice * total))
      TransactionResult(execResult, env.collectEffects)
    }
  }

  def verifySignedTx(tx: SignedTransaction,
                     id: TransactionId,
                     ep: BlockDependentEnvironment): Try[TransactionResult] = {
    cryptography
      .checkTransactionSignature(tx)
      .fold[Try[AuthorizedTransaction]](Failure(TransactionUnauthorizedException()))(Success.apply)
      .flatMap(verifyTx(_, id, ep))
  }

  def deliverOrCheckTx[R](encodedTransaction: ByteString, environmentProvider: BlockDependentEnvironment)(
      result: (Int, String) => R): Future[R] = {

    val tid = TransactionId.forEncodedTransaction(encodedTransaction)
    val `try` = Try(transcode(Bson @@ encodedTransaction.toByteArray).to[SignedTransaction])
      .flatMap(verifySignedTx(_, tid, environmentProvider))

    Future.successful {
      `try` match {
        case Success(executionResult) =>
          result(TxStatusOk, transcode(executionResult).to[Json])
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

  final case class TransactionResult(
      executionResult: ExecutionResult,
      effects: Seq[Effect]
  )

  sealed abstract class TransactionValidationException(message: String) extends Exception(message)

  final case class TransactionUnauthorizedException()
      extends TransactionValidationException("Transaction signature is invalid")

  final case class WrongWattPriceException()
      extends TransactionValidationException("Bad transaction parameter: wattPrice")

  final case class WrongWattLimitException()
      extends TransactionValidationException("Bad transaction parameter: wattLimit")

  final val TxStatusOk = 0
  final val TxStatusUnauthorized = 1
  final val TxStatusError = 2

  final case class StoredProgram(code: ByteString, owner: Address, `sealed`: Boolean)

  final class BlockDependentEnvironment(db: DB) {

    private var fee = NativeCoin.zero
    private val operations = mutable.Buffer.empty[Operation]
    private val effectsMap = mutable.Buffer.empty[(TransactionId, mutable.Buffer[Effect])]
    private val events = mutable.Buffer.empty[Effect.Event]
    private val cache = mutable.Map.empty[String, Option[Array[Byte]]]

    private lazy val blockProgramsPath = new CachedDbPath(new PureDbPath(db, "program"), cache, operations)
    private lazy val blockEffectsPath = new CachedDbPath(new PureDbPath(db, "effects"), cache, operations)
    private lazy val eventsPath = new CachedDbPath(new PureDbPath(db, "events"), cache, operations)
    private lazy val blockBalancesPath = new CachedDbPath(new PureDbPath(db, "balance"), cache, operations)

    def transactionEnvironment(executor: Address, tid: TransactionId): TransactionDependentEnvironment = {
      val effects = mutable.Buffer.empty[Effect]
      effectsMap += (tid -> effects)
      new TransactionDependentEnvironment(executor, tid, effects)
    }

    final class TransactionDependentEnvironment(val executor: Address,
                                                transactionId: TransactionId,
                                                effects: mutable.Buffer[Effect])
        extends Environment {

      private val transactionOperations = mutable.Buffer.empty[Operation]
      private val transactionEffects = mutable.Buffer.empty[Effect]
      private val transactionCache = mutable.Map.empty[String, Option[Array[Byte]]]

      private lazy val transactionProgramsPath =
        new CachedDbPath(blockProgramsPath, transactionCache, transactionOperations)
      private lazy val transactionBalancesPath =
        new CachedDbPath(blockBalancesPath, transactionCache, transactionOperations)

      def commitTransaction(): Unit = {
        operations ++= transactionOperations
        cache ++= transactionCache
        effects ++= transactionEffects
        events ++= transactionEffects.collect { case ev: Effect.Event => ev }
      }

      import Effect._

      private final class WsProgramStorage(address: Address, dbPath: DbPath) extends Storage {

        def get(key: Data): Option[Data] = {
          val hexKey = byteUtils.byteString2hex(key.toByteString)
          val value = dbPath.getRawBytes(hexKey)
          transactionEffects += StorageRead(address, key, value.map(Data.fromBytes))
          value.map(Data.fromBytes)
        }

        def put(key: Data, value: Data): Option[Data] = {
          val hexKey = byteUtils.byteString2hex(key.toByteString)
          val array = value.toByteString.toByteArray
          val prev = dbPath.putRawBytes(hexKey, array)
          transactionEffects += StorageWrite(address, key, prev.map(Data.fromBytes), Data.fromBytes(array))
          prev.map(Data.fromBytes)
        }

        def delete(key: Data): Option[Data] = {
          val hexKey = byteUtils.byteString2hex(key.toByteString)
          val value = dbPath.remove(hexKey)
          transactionEffects += StorageRemove(address, key, value.map(Data.fromBytes))
          value.map(Data.fromBytes)
        }
      }

      def createProgram(owner: Address, code: ByteString): Address = {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val addressBytes = sha256.digest(transactionId.concat(code).toByteArray)
        val address = Address @@ ByteString.copyFrom(addressBytes)
        val sp = StoredProgram(code, owner, `sealed` = false)

        transactionProgramsPath.put(byteUtils.bytes2hex(addressBytes), sp)
        transactionEffects += ProgramCreate(address, Data.Primitive.Bytes(code))
        address
      }

      def sealProgram(address: Address): Unit = {
        val oldSb = getStoredProgram(address).getOrElse(throw vm.ThrowableVmError(Error.NoSuchProgram))
        val sp = oldSb.copy(`sealed` = true)
        transactionProgramsPath.put(byteUtils.byteString2hex(address), sp)
        transactionEffects += ProgramSeal(address)
      }

      def updateProgram(address: Address, code: ByteString): Unit = {
        val oldSb = getStoredProgram(address).getOrElse(throw vm.ThrowableVmError(Error.NoSuchProgram))
        if (oldSb.`sealed`) throw vm.ThrowableVmError(Error.ProgramIsSealed)
        val sp = oldSb.copy(code = code)
        transactionProgramsPath.put(byteUtils.byteString2hex(address), sp)
        transactionEffects += ProgramUpdate(address, Data.Primitive.Bytes(code))
      }

      def event(address: Address, name: String, data: MarshalledData): Unit = {
        transactionEffects += Event(address, name, data)
      }

      def transfer(from: Address, to: Address, amount: NativeCoin): Unit = {
        if (amount < 0)
          throw ThrowableVmError(Error.AmountShouldNotBeNegative)

        val currentFrom = balance(from)
        val currentTo = balance(to)

        if (currentFrom < amount)
          throw ThrowableVmError(Error.NotEnoughMoney)

        // TODO consider to add `to` balance overflow
        transactionBalancesPath.put(byteUtils.byteString2hex(from), currentFrom - amount)
        transactionBalancesPath.put(byteUtils.byteString2hex(to), currentTo + amount)
        transactionEffects += Effect.Transfer(from, to, amount)
      }

      def balance(address: Address): NativeCoin = {
        val bal =
          transactionBalancesPath.getAs[NativeCoin](byteUtils.byteString2hex(address)).getOrElse(NativeCoin.zero)
        transactionEffects += ShowBalance(address, bal)
        bal
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
          val storage = new WsProgramStorage(address, newPath)
          ProgramContext(storage, ByteBuffer.wrap(program.code.toByteArray))
        }

      def collectEffects: Seq[Effect] = transactionEffects
    }

    def appendFee(coins: NativeCoin): Unit = {
      val newFees = NativeCoin @@ (fee + coins)
      fee = newFees
    }

    def clear(): Unit = {
      operations.clear()
      effectsMap.clear()
      events.clear()
      cache.clear()
      fee = NativeCoin.zero
    }

    def withdraw(address: Address, amount: NativeCoin): Unit = {
      if (amount < 0)
        throw vm.ThrowableVmError(Error.AmountShouldNotBeNegative)

      val current = blockBalancesPath.getAs[NativeCoin](byteUtils.byteString2hex(address)).getOrElse(NativeCoin.zero)

      if (current < amount)
        throw vm.ThrowableVmError(Error.NotEnoughMoney)

      blockBalancesPath.put(byteUtils.byteString2hex(address), current - amount)
    }

    def accrue(address: Address, amount: NativeCoin): Unit = {
      if (amount < 0)
        throw vm.ThrowableVmError(Error.ProgramIsSealed)

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
        val data = effectsMap.toMap.asInstanceOf[Map[TransactionId, Seq[Effect]]]
        blockEffectsPath.put(byteUtils.bytes2hex(byteUtils.longToBytes(height)), data)
      }

      events
        .groupBy {
          case Effect.Event(address, name, data) => (address, name)
        }
        .foreach {
          case ((address, name), evs) =>
            val len = eventsPath.getAs[Long](eventKeyLength(address, name)).getOrElse(0L)
            evs.zipWithIndex.foreach {
              case (Effect.Event(_, _, data), i) =>
                eventsPath.put(eventKeyOffset(address, name, len + i.toLong), data)
            }
            eventsPath.put(eventKeyLength(address, name), len + evs.length.toLong)
        }

      db.syncBatch(operations: _*)
      clear()
    }

  }

}
