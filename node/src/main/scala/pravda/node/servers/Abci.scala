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

import com.google.protobuf.ByteString
import com.tendermint.abci._
import pravda.common.data.blockchain._
import pravda.common.{cryptography, bytes => byteUtils}
import pravda.node.clients.AbciClient
import pravda.common.data.blockchain._
import pravda.common.data.blockchain.ApplicationStateInfo
import pravda.common.data.blockchain.Transaction._
import pravda.common.serialization._
import pravda.common.serialization.json._
import pravda.node.data.serialization.protobuf._
import pravda.common.vm.{Data, Effect, ExecutionResult, MarshalledData, RuntimeException}
import pravda.node.db.{DB, Operation}
import pravda.node.persistence.BlockChainStore.balanceEntry
import pravda.node.persistence.{FileStore, _}
import pravda.vm
import pravda.vm.impl.VmImpl
import pravda.vm.{Environment, ProgramContext, Storage, _}
import pravda.common.vm.Error
import zhukov.Marshaller
import zhukov._
import zhukov.derivation._
import zhukov.Default.auto._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class Abci(applicationStateDb: DB,
           effectsDb: DB,
           abciClient: AbciClient,
           initialDistribution: Seq[CoinDistributionMember],
           validatorManager: Option[Address])(implicit ec: ExecutionContext)
    extends io.mytc.tendermint.abci.Api {

  import Abci._

  val consensusEnv = new BlockDependentEnvironment(applicationStateDb, effectsDb, validatorManager)
  val mempoolEnv = new BlockDependentEnvironment(applicationStateDb, effectsDb, validatorManager)

  // It is updated each time when EndBlock called. It is a height of the proposed block,
  // but since this value is updated after a transactions already processed, thus when a transaction
  // is processed that value will be the height of the last block
  var lastBlockHeight = 1L
  // It is updated each time when BeginBlock called.
  var proposedHeight = 1L
  // It is updated each time when BeginBlock called. The value is taken from the header
  // of the current proposed block (header is passed in BeginBlock callback)
  var proposedBlockTimestamp = 0L

  var validators: Vector[Address] = Vector.empty[Address]
  val balances = new PureDbPath(applicationStateDb, "balance")

  def info(request: RequestInfo): Future[ResponseInfo] = {
    FileStore.readApplicationStateInfoAsync().map { maybeInfo =>
      val info = maybeInfo.getOrElse(ApplicationStateInfo.Empty)
      ResponseInfo(lastBlockHeight = info.blockHeight, lastBlockAppHash = info.appHash)
    }
  }

  def initChain(request: RequestInitChain): Future[ResponseInitChain] = {

    val initValidators = request.validators.toVector
      .map(x => tendermint.unpackAddress(x.getPubKey.data))

    for {
      _ <- FileStore
        .updateApplicationStateInfoAsync(ApplicationStateInfo(lastBlockHeight, ByteString.EMPTY, initValidators, 0L))
      _ <- Future.sequence(initialDistribution.map {
        case CoinDistributionMember(address, amount) =>
          Future(balances.put(byteUtils.byteString2hex(address), amount))
      })
    } yield ResponseInitChain.defaultInstance

  }

  /*
   *  request.header contains header of the current proposed block. The `header` field has an `Option` type,
   *  but in the Protobuf specification this field is not an `optional`, so we can use `getHeader` method safely.
   */
  def beginBlock(request: RequestBeginBlock): Future[ResponseBeginBlock] = {
    consensusEnv.clear()

    // Timestamp of the current proposed block
    proposedBlockTimestamp = utils.protoTimestampToLong(request.getHeader.getTime)
    proposedHeight = request.getHeader.height

    val malicious = request.byzantineValidators.map(x => tendermint.unpackAddress(x.getValidator.address))
    val absent = request.getLastCommitInfo.votes.collect {
      case VoteInfo(validator, signedLastBlock) if !signedLastBlock =>
        validator.map(v => tendermint.unpackAddress(v.address))
    }.flatten

    FileStore
      .modifyApplicationStateInfoAsync(_.copy(blockTimestamp = proposedBlockTimestamp))
      .map { maybeInfo =>
        val info = maybeInfo.getOrElse(ApplicationStateInfo.Empty)
        validators = info.validators.filter(address => !malicious.contains(address) && !absent.contains(address))
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
    val env = ep.transactionEnvironment(tx.from, id, proposedHeight, proposedBlockTimestamp)
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
      TransactionResult(id, execResult, env.collectEffects)
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

    import pravda.node.data.serialization.json._

    val tid = TransactionId.forEncodedTransaction(encodedTransaction)
    val `try` = Try(transcode(Protobuf @@ encodedTransaction.toByteArray).to[SignedTransaction])
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
    // save the height of the current proposed block. It will be stored later as a last block height
    lastBlockHeight = request.height

    val tendermintValidatorUpdates = consensusEnv.getValidatorUpdates.toList.map {
      case (addr, power) => ValidatorUpdate(Some(PubKey("ed25519", addr)), power)
    }

    Future.successful(ResponseEndBlock.defaultInstance.withValidatorUpdates(tendermintValidatorUpdates))
  }

  def commit(request: RequestCommit): Future[ResponseCommit] = {
    consensusEnv.commit(lastBlockHeight, validators)
    mempoolEnv.clear()
    val hash = ByteString.copyFrom(applicationStateDb.stateHash)
    FileStore
      .modifyApplicationStateInfoAsync(_.copy(blockHeight = lastBlockHeight, appHash = hash, validators = validators))
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

  final case class AdditionalDataForAddress(transfersLastTransactionNumber: Long = 1L,
                                            eventsLastTransactionNumber: Long = 1L,
                                            lastTransactionNumber: Long = 1L)

  final class BlockDependentEnvironment(applicationStateDb: DB, effectsDb: DB, validatorManager: Option[Address]) {
    blockEnv =>

    private var fee = NativeCoin.zero
    private val operations = mutable.Buffer.empty[Operation]
    private val effectsMap = mutable.Buffer.empty[(TransactionId, TransactionResultInfo)]
    private val cache = mutable.Map.empty[String, Option[Array[Byte]]]

    private val validatorUpdates = mutable.Map.empty[Address, Long]

    // BEGIN Any puts in these db paths will be written in applicationState and used during appHash calculation
    private lazy val blockProgramsPath =
      new CachedDbPath(new PureDbPath(applicationStateDb, "program"), cache, operations)
    private lazy val blockBalancesPath =
      new CachedDbPath(new PureDbPath(applicationStateDb, "balance"), cache, operations)
    private lazy val eventsPath = new CachedDbPath(new PureDbPath(applicationStateDb, "events"), cache, operations)
    // END

    private lazy val txIdIndexPath =
      new CachedDbPath(new PureDbPath(effectsDb, "txIdIndex"), cache, operations)
    private lazy val blockEffectsPath = new CachedDbPath(new PureDbPath(effectsDb, "effects"), cache, operations)
    private lazy val transactionsByAddressPath =
      new CachedDbPath(new PureDbPath(effectsDb, "transactionsByAddress"), cache, operations)
    private lazy val transferEffectsByAddressPath =
      new CachedDbPath(new PureDbPath(effectsDb, "transferEffectsByAddress"), cache, operations)
    private lazy val eventsByAddressPath =
      new CachedDbPath(new PureDbPath(effectsDb, "eventsByAddress"), cache, operations)
    private lazy val additionalDataByAddressPath =
      new CachedDbPath(new PureDbPath(effectsDb, "additionalDataByAddress"), cache, operations)

    def transactionEnvironment(executor: Address,
                               tid: TransactionId,
                               proposedHeight: Long = 0,
                               proposedBlockTimestamp: Long = 0): TransactionDependentEnvironment = {
      val effects = mutable.Buffer.empty[Effect]
      effectsMap += tid -> TransactionResultInfo(proposedBlockTimestamp, effects)
      new TransactionDependentEnvironment(executor, proposedHeight, proposedBlockTimestamp, tid, effects)
    }

    final class TransactionDependentEnvironment(val executor: Address,
                                                proposedHeight: Long,
                                                proposedBlockTimestamp: Long,
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

        storeTransactionsToAddress(transactionsByAddressPath, executor, transactionId, transactionEffects)
        storeTransferEffectsToAddress(transferEffectsByAddressPath, executor, transactionId, transactionEffects)
        storeEventsToAddress(eventsByAddressPath, executor, transactionId, transactionEffects)
      }

      private def putByOffset[A: Marshaller](dbPath: DbPath, address: Address, offset: Long, objToStore: A): Unit = {
        val key = keyWithOffset(byteUtils.byteString2hex(address), offset)
        dbPath.put(key, objToStore)
      }

      private def getAdditionalDataByAddress(addr: Address): AdditionalDataForAddress =
        additionalDataByAddressPath
          .getAs[AdditionalDataForAddress](byteUtils.byteString2hex(addr))
          .fold(AdditionalDataForAddress())(identity)

      private def storeTransactionsToAddress(dbPath: DbPath,
                                             executor: Address,
                                             transactionId: TransactionId,
                                             transactionEffects: Seq[Effect]): Unit = {
        val additionalData = getAdditionalDataByAddress(executor)
        val num = additionalData.lastTransactionNumber
        putByOffset(
          dbPath,
          executor,
          num - 1,
          TransactionEffects.AllEffects(num, proposedHeight, proposedBlockTimestamp, transactionId, transactionEffects))

        additionalDataByAddressPath.put(
          byteUtils.byteString2hex(executor),
          additionalData.copy(lastTransactionNumber = additionalData.lastTransactionNumber + 1L))
      }

      private def storeTransferEffectsToAddress(dbPath: DbPath,
                                                executor: Address,
                                                transactionId: TransactionId,
                                                transferEffects: Seq[Effect]): Unit = {
        def storeTransferEffects(dbPath: DbPath,
                                 address: Address,
                                 transactionId: TransactionId,
                                 transfers: Seq[Effect.Transfer]): Unit = {
          val additionalData = getAdditionalDataByAddress(address)
          val num = additionalData.transfersLastTransactionNumber

          putByOffset(
            dbPath,
            address,
            num - 1,
            TransactionEffects.Transfers(num, proposedHeight, proposedBlockTimestamp, transactionId, transfers))

          additionalDataByAddressPath.put(byteUtils.byteString2hex(address),
                                          additionalData.copy(transfersLastTransactionNumber = num + 1L))
        }

        val executorTransferEffects: Seq[Effect.Transfer] = transferEffects.collect {
          case e @ Effect.Transfer(from, _, _) if from == executor => e
        }

        val transferToAnotherAddress: Seq[(Address, Effect.Transfer)] = transferEffects.collect {
          case e @ Effect.Transfer(_, to, _) => (to, e)
        }

        // 1) Add Transfer effects to the address, who signed the corresponding transaction (aka executor)
        if (executorTransferEffects.nonEmpty) {
          storeTransferEffects(dbPath, executor, transactionId, executorTransferEffects)
        }

        // 2) Add Transfer effects which affect to another address
        if (transferToAnotherAddress.nonEmpty) {
          transferToAnotherAddress.groupBy(_._1).foreach {
            case (addr, transfers) =>
              storeTransferEffects(dbPath, addr, transactionId, transfers.map(_._2))
          }
        }
      }

      private def storeEventsToAddress(dbPath: DbPath,
                                       executor: Address,
                                       transactionId: TransactionId,
                                       effects: Seq[Effect]): Unit = {
        def storeEvents(dbPath: DbPath,
                        address: Address,
                        transactionId: TransactionId,
                        events: Seq[Effect.Event]): Unit = {
          val additionalData = getAdditionalDataByAddress(address)
          val num = additionalData.eventsLastTransactionNumber
          putByOffset(
            dbPath,
            address,
            num - 1,
            TransactionEffects.ProgramEvents(num, proposedHeight, proposedBlockTimestamp, transactionId, events))

          additionalDataByAddressPath.put(byteUtils.byteString2hex(address),
                                          additionalData.copy(eventsLastTransactionNumber = num + 1L))
        }

        val events = effects collect {
          case e: Effect.Event => e
        }

        if (events.nonEmpty) {
          storeEvents(dbPath, executor, transactionId, events)
        }
      }

      import Effect._

      private final class WsProgramStorage(address: Address, dbPath: DbPath) extends Storage {

        def get(key: Data.Primitive): Option[Data] = {
          val hexKey = byteUtils.byteString2hex(key.toByteString)
          val value = dbPath.getRawBytes(hexKey)
          transactionEffects += StorageRead(address, key, value.map(Data.fromBytes))
          value.map(Data.fromBytes)
        }

        def put(key: Data.Primitive, value: Data): Option[Data] = {
          val hexKey = byteUtils.byteString2hex(key.toByteString)
          val array = value.toByteString.toByteArray
          val prev = dbPath.putRawBytes(hexKey, array)
          transactionEffects += StorageWrite(address, key, prev.map(Data.fromBytes), Data.fromBytes(array))
          prev.map(Data.fromBytes)
        }

        def delete(key: Data.Primitive): Option[Data] = {
          val hexKey = byteUtils.byteString2hex(key.toByteString)
          val value = dbPath.remove(hexKey)
          transactionEffects += StorageRemove(address, key, value.map(Data.fromBytes))
          value.map(Data.fromBytes)
        }
      }

      def createProgram(address: Address, code: ByteString): Unit = {
        val sp = StoredProgram(code, `sealed` = false)
        transactionProgramsPath.put(byteUtils.byteString2hex(address), sp)
        transactionEffects += ProgramCreate(address, Data.Primitive.Bytes(code))
      }

      def sealProgram(address: Address): Unit = {
        val oldSb = getStoredProgram(address).getOrElse(throw vm.ThrowableVmError(Error.NoSuchProgram))
        val sp = oldSb.copy(`sealed` = true)
        transactionProgramsPath.put(byteUtils.byteString2hex(address), sp)
        transactionEffects += ProgramSeal(address)
      }

      def updateProgram(address: Address, code: ByteString): Unit = {
        val oldSb = getStoredProgram(address).getOrElse(throw vm.ThrowableVmError(Error.NoSuchProgram))
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

      def getProgram(address: Address): Option[ProgramContext] =
        getStoredProgram(address) map { program =>
          val newPath = transactionProgramsPath :+ byteUtils.byteString2hex(address)
          val storage = new WsProgramStorage(address, newPath)
          ProgramContext(storage, program.code, program.`sealed`)
        }

      def collectEffects: Seq[Effect] = transactionEffects

      def chainHeight: Long = {
        FileStore
          .readApplicationStateInfo()
          .fold(throw ThrowableVmError(Error.NoInfoAboutAppState))(_.blockHeight)
      }

      def lastBlockHash: ByteString = {
        FileStore
          .readApplicationStateInfo()
          .fold(throw ThrowableVmError(Error.NoInfoAboutAppState))(_.appHash)
      }

      def lastBlockTime: Long = {
        FileStore
          .readApplicationStateInfo()
          .fold(throw ThrowableVmError(Error.NoInfoAboutAppState))(_.blockTimestamp)
      }

      def updateValidator(validator: Address, power: Long): Unit = {
        blockEnv.updateValidator(executor, validator, power)
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
      validatorUpdates.clear()
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
      blockBalancesPath.put[NativeCoin](byteUtils.byteString2hex(address), NativeCoin @@ (current + amount))
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
        val data = effectsMap.toMap.asInstanceOf[Map[TransactionId, TransactionResultInfo]]
        blockEffectsPath.put(byteUtils.bytes2hex(byteUtils.longToBytes(height)), Tuple1(data))
        // TODO zhukov will probably support raw Maps without wrapper
      }

      val txIndex = mutable.Map[TransactionId, mutable.Buffer[(Address, Long)]]()

      effectsMap
        .flatMap {
          case (tx, trInfo) =>
            trInfo.effects collect {
              case event: Effect.Event => (tx, (trInfo.timestamp, event))
            }
        }
        .groupBy {
          case (_, (_, Effect.Event(address, _, _))) => address
        }
        .foreach {
          case (address, evs) =>
            val len = eventsPath.getAs[Long](eventKeyLength(address)).getOrElse(0L)
            evs.zipWithIndex.foreach {
              case ((tx, (timestamp, Effect.Event(_, name, data))), i) =>
                txIndex.get(tx) match {
                  case Some(buf) => buf += ((address, len + i.toLong))
                  case None      => txIndex(tx) = mutable.Buffer((address, len + i.toLong))
                }
                eventsPath.put(eventKeyOffset(address, len + i.toLong), (tx, timestamp, name, data))
            }
            eventsPath.put(eventKeyLength(address), len + evs.length.toLong)
        }

      txIndex.foreach {
        case (txId, offsets) =>
          val len = txIdIndexPath.getAs[Long](transactionIdKeyLength(txId)).getOrElse(0L)
          offsets.zipWithIndex.foreach {
            case (offset, i) =>
              txIdIndexPath.put(transactionIdKeyOffset(txId, len + i.toLong), offset)
          }
          txIdIndexPath.put(transactionIdKeyLength(txId), len + offsets.length.toLong)
      }

      applicationStateDb.applyBatchOperations(operations: _*)
      effectsDb.applyBatchOperations(operations: _*)
      clear()
    }

    def updateValidator(executor: Address, validator: Address, power: Long): Unit = {
      if (validatorManager.contains(executor)) {
        validatorUpdates(validator) = power
      } else {
        throw vm.ThrowableVmError(Error.NonValidatorManager)
      }
    }

    def getValidatorUpdates: Map[Address, Long] = validatorUpdates.toMap
  }

  def keyWithOffset(key: String, offset: Long) = f"$key:$offset%016x"
}
