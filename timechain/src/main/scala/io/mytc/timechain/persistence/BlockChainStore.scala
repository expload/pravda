package io.mytc
package timechain.persistence


import com.google.protobuf.ByteString
import io.mytc.keyvalue.{DB, Operation}
import io.mytc.timechain.data.blockchain.{Transaction, TransactionData}
import io.mytc.timechain.data.common._
import io.mytc.timechain.data.domain._
import io.mytc.timechain.data.misc.BlockChainInfo
import io.mytc.timechain.data.processing.{ProcessingEffect, ProcessingState}
import shapeless.{::, HNil}

import scala.concurrent.Future

import implicits._

import scala.concurrent.ExecutionContext.Implicits.global

object BlockChainStore {
  def apply(path: String) = new BlockChainStore(path)
}

class BlockChainStore(path: String) {

  type FOpt[A] = Future[Option[A]]

  private implicit val db = DB(path, hashCounter = true)
  db.initHash

  private val multiplierEntry = Entry[Address, BigDecimal]("multiplier")
  private val accountEntry = Entry[Address, Account]("account")
  private val depositEntry = Entry[DepositId, Deposit]("deposit")
  private val tariffMatrixEntry = Entry[Address, TariffMatrix]("matrix")
  private val punishmentEntry = Entry[Address :: DataRef :: HNil, Null]("punishment")
  private val confirmationEntry = Entry[Address :: DataRef :: HNil, Null]("confirmation")
  private val blockChainInfoEntry = SingleEntry[BlockChainInfo]("blockchain")
  private val organizationEntry = Entry[Address, Organization]("organization")
  private val offerEntry = Entry[DataRef, Offer]("offer")
  private val datarefEntry = Entry[Address :: DataRef :: HNil, DataRef]("dataref")

  def isConfirmed(address: Address, data: DataRef): Future[Boolean] = {
    confirmationEntry.contains(address::data::HNil)
  }

  private def setConfirmed(address: Address, data: DataRef, state: BatchState): BatchState = {
    state.addOperations(confirmationEntry.putBatch(address::data::HNil))
  }

  def isPunished(address: Address, data: DataRef): Future[Boolean] = {
    punishmentEntry.contains(address::data::HNil)
  }

  private def setPunishment(address: Address, data: DataRef, state: BatchState): BatchState = {
    state.addOperations(punishmentEntry.putBatch(address::data::HNil))
  }


  private def putMatrix(address: Address, matrix: TariffMatrix, state: BatchState): BatchState = {
    state.addOperations(tariffMatrixEntry.putBatch(address, matrix))
  }

  def getMatrix(address: Address): FOpt[TariffMatrix] = {
    tariffMatrixEntry.get(address)
  }

  def getMatrices: Future[List[TariffMatrix]] = {
    tariffMatrixEntry.all
  }

  private def putMultiplier(address: Address, multiplier: BigDecimal, state: BatchState): BatchState = {
    state.addOperations(multiplierEntry.putBatch(address, multiplier))
  }

  def getMultiplier(address: Address): FOpt[BigDecimal] = {
    multiplierEntry.get(address)
  }

  def getDeposit(id: DepositId): FOpt[Deposit] = {
    depositEntry.get(id)
  }

  private def addOrCreateDeposit(id: DepositId, amount: BigDecimal, state: BatchState):
          BatchState = {
      val deposit = state.deposits.get(id).orElse(depositEntry.syncGet(id))
      val newDeposit = deposit.map(
        d => d.copy(amount = d.amount + amount, block = state.newHeight)
      ).getOrElse(Deposit(id, amount, state.newHeight))
      state.addOperations(depositEntry.putBatch(id, newDeposit)).putDeposit(newDeposit)
  }

  private def putAccount(account: Account, state: BatchState): BatchState =  {
    state.addOperations(accountEntry.putBatch(account.address, account))
  }

  def getAccount(address: Address): FOpt[Account] = {
    accountEntry.get(address)
  }

  private def addOrCreateAccount(
                                  address: Address,
                                  free: BigDecimal,
                                  frozen: BigDecimal,
                                  state: BatchState
                                ): BatchState = {
    val account = state.accounts.get(address).orElse(accountEntry.syncGet(address))
    val newAccount = account.map(
      a => a.copy(free = a.free + free, frozen = a.frozen + frozen)
    ).getOrElse(Account(address, Mytc(free), Mytc(frozen)))
    state.addOperations(accountEntry.putBatch(address, newAccount)).putAccount(newAccount)
  }

  def getBlockchainInfo(): Future[BlockChainInfo] = {
    blockChainInfoEntry.get().map {
      _.getOrElse(BlockChainInfo(0, Vector.empty))
    }
  }

  def putBlockChainInfo(blockChainInfo: BlockChainInfo): Future[Unit] = {
    blockChainInfoEntry.put(blockChainInfo)
  }

  private def putBlockChainInfo(blockChainInfo: BlockChainInfo, state: BatchState): BatchState =  {
    state.addOperations(blockChainInfoEntry.putBatch(blockChainInfo))
  }

  private def putOffer(offer: Offer, state: BatchState): BatchState = {
    state.addOperations(
      offerEntry.putBatch(offer.dataRef, offer),
      datarefEntry.putBatch(offer.seller :: offer.dataRef :: HNil, offer.dataRef)
    )
  }

  def getOffer(dataRef: DataRef): FOpt[Offer] = {
    offerEntry.get(dataRef)
  }

  private def updateOffer(dataRef: DataRef, update: Offer => Offer, state: BatchState): BatchState = {
    state.offers.get(dataRef).orElse(
      offerEntry.syncGet(dataRef)
    ).map(update).map {
      of => state.putOffer(of).addOperations(offerEntry.putBatch(dataRef, of))
    }.getOrElse(state)
  }

  def getOffersByUser(address: Address): Future[List[Offer]] = {
    datarefEntry.startsWith(address).flatMap( f =>
      Future.sequence {
        f.map {
          x => offerEntry.get(x).map(_.get)
        }
      }
    )
  }


  def getOrganization(address: Address): Future[Option[Organization]] = {
    organizationEntry.get(address)
  }

  private def putOrganization(organization: Organization, state: BatchState): BatchState = {
    state.addOperations(organizationEntry.putBatch(organization.address, organization))
  }

  import ProcessingEffect._

  def restore[I, V](alreadyExists: ProcessingState => (I => Boolean))
                   (update: (ProcessingState, I, V) => ProcessingState)
                   (restore: I => Future[Option[V]]): (ProcessingState, I) => Future[ProcessingState] = {
    (state, id) =>
      if (alreadyExists(state)(id)) {
        Future.successful(state)
      } else {
        restore(id).map {
          _.map {
            v => update(state, id, v)
          }.getOrElse(state)
        }
      }
  }

  def restoreProcessingState(tx: Transaction, state: ProcessingState): Future[ProcessingState] = {

    val restoreDeposit = restore[DepositId, Deposit](_.deposits.contains)(
      (s, id, o) => s.copy(deposits = s.deposits + (id -> o))
    )(getDeposit)
    val restoreAccount = restore[Address, Account](_.accounts.contains)(
      (s, i, a) => s.copy(accounts = s.accounts + (i -> a))
    )(getAccount)
    val restoreOffer = restore[DataRef, Offer](_.offers.contains)(
      (s, i, o) => s.copy(offers = s.offers + (i -> o))
    )(getOffer)
    val restorePunishment = restore[Punishment, Punishment](_.punished.contains)(
      (s, _, o) => s.copy(punished = s.punished + o)
    )(p => isPunished(p.customer, p.dataRef).map(if(_) Some(p) else None))
    val restoreConfirmation = restore[Confirmation, Confirmation](_.confirmed.contains)(
      (s, _, o) => s.copy(confirmed = s.confirmed + o)
    )(c => isPunished(c.customer, c.dataRef).map(if(_) Some(c) else None))
    val restoreTariffMatrix = restore[Address, TariffMatrix](_.tariffMatrices.contains)(
      (s, id, o) => s.copy(tariffMatrices = s.tariffMatrices + (id -> o))
    )(getMatrix)
    val restoreMultiplier = restore[Address, BigDecimal](_.multipliers.contains)(
      (s, id, o) => s.copy(multipliers = s.multipliers + (id -> o))
    )(getMultiplier)

    tx.data match {
      case data: TransactionData.Time =>
        for {
          state <- restoreAccount(state, tx.from)
          state <- restoreAccount(state, data.user)
          state <- restoreTariffMatrix(state, tx.from)
        } yield state
      case data: TransactionData.Transfer =>
        for {
          state <- restoreAccount(state, tx.from)
          state <- restoreAccount(state, data.to)
        } yield state
      case data: TransactionData.DataPurchasingDeposit =>
        for {
          state <- restoreAccount(state, data.vendor)
          state <- restoreAccount(state, tx.from)
          state <- restoreDeposit(state, DepositId(tx.from, data.vendor))
        } yield state
      case data: TransactionData.DataPurchasingConfirmation =>
        for {
          state  <- restoreAccount(state, tx.from)
          state <- restoreOffer(state, data.dataRef)
          offer = state.offers(data.dataRef)
          state <- restoreTariffMatrix(state, offer.seller)
          state <- restoreMultiplier(state, tx.from)
          state <- restoreDeposit(state, DepositId(tx.from, offer.seller))
          state <- restoreAccount(state, offer.seller)
          state <- restoreAccount(state, offer.user)
          state <- restorePunishment(state, Punishment(tx.from, data.dataRef))
          state <- restoreConfirmation(state, Confirmation(tx.from, data.dataRef))
        } yield state
      case data: TransactionData.CheatingCustomerPunishment =>
        for {
          state <- restoreAccount(state, tx.from)
          state <- restoreTariffMatrix(state, tx.from)
          state <- restoreMultiplier(state, tx.from)
          state <- restoreOffer(state, data.intention.data.dataRef)
          state <- restoreDeposit(state, DepositId(data.intention.data.address, tx.from))
          state <- restorePunishment(state, Punishment(data.intention.data.address, data.intention.data.dataRef))
          state  <- restoreConfirmation(state, Confirmation(data.intention.data.address, data.intention.data.dataRef))
        } yield state
      case data: TransactionData.MultiplierUpdating =>
        for {
          state  <- restoreAccount(state, tx.from)
        } yield state
      case data: TransactionData.TariffMatrixUpdating =>
        for {
          state  <- restoreAccount(state, tx.from)
          state  <- restoreTariffMatrix(state, tx.from)
        } yield state
      case _ =>
        restoreAccount(state, tx.from)
    }
  }

  case class BatchState(
    newHeight: Long,
    validators: Vector[Address],
    offers: Map[DataRef, Offer] = Map.empty,
    accounts: Map[Address, Account] = Map.empty,
    deposits: Map[DepositId, Deposit] = Map.empty,
    operations: Vector[Operation] = Vector.empty
  ) {
    def addOperations(ops: Operation*): BatchState = copy(operations = operations ++ ops)
    def putOffer(offer: Offer): BatchState = copy(offers = offers + (offer.dataRef -> offer))
    def putAccount(account: Account): BatchState = copy(accounts = accounts + (account.address -> account))
    def putDeposit(deposit: Deposit): BatchState = copy(deposits = deposits + (deposit.id -> deposit))
  }

  def applyPersistentEffect(effect: ProcessingEffect, state: BatchState): BatchState = {
    effect match {
      case CheckDomainAndUpdateInfo(domain, address) =>
        putOrganization(Organization(address, domain), state)
      case IncrementOfferPurchaseCount(dataRef) =>
        updateOffer(dataRef, x => x.copy(purchaseCount = x.purchaseCount + 1), state)
      case ValidatorsReward(amount) =>
        if (state.validators.isEmpty) {
          println("empty validators list")
          state
        } else {
          val share = amount / state.validators.length
          state.validators.foldLeft(state)((st, v) => addOrCreateAccount(v, share, 0, st))
        }
      case NewOffer(offer) => putOffer(offer, state)
      case NewAccount(account) => putAccount(account, state)
      case Accrue(account, amount) => addOrCreateAccount(account, amount, 0, state)
      case Withdraw(account, amount) => addOrCreateAccount(account, -amount, 0, state)
      case Freeze(account, amount) => addOrCreateAccount(account, -amount, amount, state)
      case Unfreeze(account, amount) => addOrCreateAccount(account, amount, -amount, state)
      case AccrueDeposit(id, amount) => addOrCreateDeposit(id, amount, state)
      case WithdrawDeposit(id, amount) => addOrCreateDeposit(id, -amount, state)
      case Punish(punishment) => setPunishment(punishment.customer, punishment.dataRef, state)
      case Confirm(confirmation) => setConfirmed(confirmation.customer, confirmation.dataRef, state)
      case UpdateTariffMatrix(tariffMatrix) => putMatrix(tariffMatrix.vendor, tariffMatrix, state)
      case SetMultiplier(vendor, value) => putMultiplier(vendor, value, state)
    }
  }

  def applyPersistentEffects(effects: Seq[ProcessingEffect], validators: Vector[Address], newHeight: Long): Future[ByteString] = {
    getBlockchainInfo().flatMap {
      bcInfo =>
        val initState = putBlockChainInfo(bcInfo.copy(height = newHeight), BatchState(newHeight, validators))
        val state = effects.foldLeft(initState) {
          case (st, effect) =>
            applyPersistentEffect(effect, st)
        }
        db.batch(state.operations:_*).map { _ =>
          appHash
        }
    }
  }

  def appHash: ByteString = {
    if(db.stateHash.forall(_ == 0)) ByteString.EMPTY
    else ByteString.copyFrom(db.stateHash)
  }


  def close(): Unit = {
    db.close()
  }

}
