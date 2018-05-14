package io.mytc.timechain.data

import com.google.protobuf.ByteString
import blockchain._
import common._
import io.mytc.timechain.contrib.ripemd160
import io.mytc.timechain.data.misc.BlockChainInfo
import io.mytc.timechain.data.blockchain.Transaction.AuthorizedTransaction

import scala.annotation.switch
import io.mytc.timechain.data.blockchain.TransactionData
import io.mytc.timechain.data.offchain.PurchaseIntention
import io.mytc.timechain.data.common.DataRef
import io.mytc.timechain.data.domain.Multiplier
import io.mytc.timechain.data.offchain.PurchaseIntention.AuthorizedPurchaseIntention
import io.mytc.timechain.data.processing.{accountByAddress, checkFreeFunds, checkPositive}

import scala.sys.process.Process

object processing {

  final val UserShare = BigDecimal(0.3)
  final val VendorShare = BigDecimal(0.7)
  final val DepositWithdrawTimeout = 500L // TODO: it should be set properly

  def processTransaction(tx: AuthorizedTransaction, state: ProcessingState): ProcessingResult = {
    tx.data match {
      case x: TransactionData.Time => processTime(tx.from, tx.fee, x, state)
      case x: TransactionData.Transfer => processTransfer(tx.from, tx.fee, x, state)
      case x: TransactionData.Distribution => processDistribution(tx.from, x, state)
      case x: TransactionData.DataPurchasingDeposit => processDataPurchasingDeposit(tx.from, tx.fee, x, state)
      case x: TransactionData.DataPurchasingConfirmation => processDataPurchasingConfirmation(tx.from, tx.fee, x, state)
      case x: TransactionData.ThisIsMe => processThisIsMe(tx.from, tx.fee, x, state)
      case x: TransactionData.CheatingCustomerPunishment => processCheatingCustomerPunishment(tx.from, x, tx.fee, state)
      case x: TransactionData.TariffMatrixUpdating => processTariffMatrixUpdating(tx.from, x, tx.fee, state)
      case x: TransactionData.MultiplierUpdating => processMultiplierUpdating(tx.from, x, tx.fee, state)
    }
  }

  def processMultiplierUpdating(from: Address, tx: TransactionData.MultiplierUpdating,
                                fee: Mytc, state: ProcessingState): ProcessingResult = {
    for {
      fromAccount <- accountByAddress(from, state)
      _ <- checkPositive(fee)
      _ <- checkFreeFunds(fromAccount, fee)
      _ <- checkNotNegative(tx.value)
    } yield {
      List(
        ProcessingEffect.ValidatorsReward(fee),
        ProcessingEffect.Withdraw(from, fee),
        ProcessingEffect.SetMultiplier(from, tx.value)
      )
    }
  }

  def processTariffMatrixUpdating(from: Address, tx: TransactionData.TariffMatrixUpdating,
                                  fee: Mytc, state: ProcessingState): ProcessingResult = {
    for {
      fromAccount <- accountByAddress(from, state)
      _ <- checkPositive(fee)
      _ <- checkFreeFunds(fromAccount, fee)
      _ <- validateMatrix(tx.tariffMatrix)
      matrix = mergedMatix(tx.tariffMatrix, state.tariffMatrices.get(from))
    } yield {
      List(
        ProcessingEffect.ValidatorsReward(fee),
        ProcessingEffect.Withdraw(from, fee),
        ProcessingEffect.UpdateTariffMatrix(matrix)
      )
    }
  }

  def processCheatingCustomerPunishment(from: Address, tx: TransactionData.CheatingCustomerPunishment, fee: Mytc, state: ProcessingState): ProcessingResult = {
    val cheater = tx.intention.data.address
    val did = DepositId(cheater, from)
    val multiplier = multiplierByAddress(from, state)
    for {
      fromAccount <- accountByAddress(from, state)
      _ <- checkIntention(tx.intention)
      offer <- offerByDataRef(tx.intention.data.dataRef, state)
      tariffMatrix <- tariffMatrixByAddress(from, state)
      price = tariffMatrix.price(
        offer.tariff,
        state.lastBlockHeight - offer.blockHeight,
        1, // TODO: Market Rate is always 1 by now, we need to calculate it properly
        multiplier
      )
      deposit <- depositById(did, state)
      _ <- checkPositive(fee)
      _ <- checkDeposit(deposit, price)
      _ <- checkFreeFunds(fromAccount, fee)
      _ <- checkIfNotPunished(cheater, offer.dataRef, state)
      _ <- checkIfNotConfirmed(cheater, offer.dataRef, state)
    } yield {
      List(
        ProcessingEffect.ValidatorsReward(fee),
        ProcessingEffect.Withdraw(from, fee),
        ProcessingEffect.WithdrawDeposit(did, price),
        ProcessingEffect.Punish(domain.Punishment(cheater, tx.intention.data.dataRef))
      )
    }
  }

  def processThisIsMe(from: Address, fee: Mytc, tx: TransactionData.ThisIsMe, state: ProcessingState): ProcessingResult = {
    for {
      fromAccount <- accountByAddress(from, state)
      _ <- checkPositive(fee)
      _ <- checkFreeFunds(fromAccount, fee)
    } yield {
      List(
        ProcessingEffect.ValidatorsReward(fee),
        ProcessingEffect.Withdraw(from, fee),
        ProcessingEffect.CheckDomainAndUpdateInfo(tx.domain, from)
      )
    }
  }

  def processDataPurchasingConfirmation(from: Address, fee: Mytc, tx: TransactionData.DataPurchasingConfirmation, state: ProcessingState): ProcessingResult = {
    for {
      fromAccount <- accountByAddress(from, state)
      offer <- offerByDataRef(tx.dataRef, state)
      did = DepositId(from, offer.seller)
      deposit <- depositById(did, state)
      tariffMatrix <- tariffMatrixByAddress(offer.seller, state)
      multiplier = multiplierByAddress(offer.seller, state)
      price = tariffMatrix.price(
        offer.tariff,
        state.lastBlockHeight - offer.blockHeight,
        1, // TODO: Market Rate is always 1 by now, we need to calulate it properly
        multiplier
      )
      _ <- checkPositive(fee)
      _ <- checkFrozenFunds(fromAccount, price)
      _ <- checkFreeFunds(fromAccount, fee)
      _ <- checkDeposit(deposit, price)
      _ <- checkIfNotPunished(from, tx.dataRef, state)
      _ <- checkIfNotConfirmed(from, tx.dataRef, state)
    } yield {
      val xs = List(
        ProcessingEffect.ValidatorsReward(fee),
        ProcessingEffect.IncrementOfferPurchaseCount(offer.dataRef),
        ProcessingEffect.WithdrawDeposit(did, price),
        ProcessingEffect.Unfreeze(from, price),
        ProcessingEffect.Withdraw(from, Mytc(fee + price)),
        ProcessingEffect.Accrue(offer.seller, Mytc(price * VendorShare)),
        ProcessingEffect.Accrue(offer.user, Mytc(price * UserShare)),
        ProcessingEffect.Confirm(domain.Confirmation(from, tx.dataRef))
      )
      if (state.accounts.contains(offer.user)) xs
      else ProcessingEffect.NewAccount(newAccount(offer.user)) :: xs
    }
  }

  def processDataPurchasingDeposit(from: Address, fee: Mytc, tx: TransactionData.DataPurchasingDeposit, state: ProcessingState): ProcessingResult = {
    for {
      fromAccount <- accountByAddress(from, state)
      requiredFunds = common.Mytc(tx.amount + fee)
      _ <- checkPositive(tx.amount, fee)
      _ <- checkFreeFunds(fromAccount, requiredFunds)
    } yield {
      List(
        ProcessingEffect.ValidatorsReward(fee),
        ProcessingEffect.Freeze(from, tx.amount),
        ProcessingEffect.AccrueDeposit(DepositId(from, tx.vendor), tx.amount),
        ProcessingEffect.Withdraw(from, fee)
      )
    }
  }

  def processTransfer(from: Address, fee: Mytc, tx: TransactionData.Transfer, state: ProcessingState): ProcessingResult = {
    for {
      fromAccount <- accountByAddress(from, state)
      requiredFunds = common.Mytc(tx.amount + fee)
      _ <- checkPositive(tx.amount, fee)
      _ <- checkFreeFunds(fromAccount, requiredFunds)
    } yield {
      val effects = List(
        ProcessingEffect.ValidatorsReward(fee),
        ProcessingEffect.Withdraw(from, requiredFunds),
        ProcessingEffect.Accrue(tx.to, tx.amount)
      )
      if (state.accounts.contains(tx.to)) effects
      else ProcessingEffect.NewAccount(newAccount(tx.to)) :: effects
    }
  }

  def processDistribution(from: Address, tx: TransactionData.Distribution, state: ProcessingState): ProcessingResult = {
    (state.lastBlockHeight: @switch) match {
      case 1 =>
        val effects = tx.accounts flatMap {
          case (address, amount) =>
            List(
              ProcessingEffect.NewAccount(newAccount(address)),
              ProcessingEffect.Accrue(address, amount)
            )
        }
        Right(effects)
      case _ => Left(ProcessingError.WrongEra)
    }
  }

  def processTime(from: Address, fee: Mytc, tx: TransactionData.Time, state: ProcessingState): ProcessingResult =
    for {
      sellerAccount <- accountByAddress(from, state)
      tariffMatrix <- tariffMatrixByAddress(from, state)
      requiredFunds = common.Mytc(tx.reward + fee)
      _ <- validateTariff(tx.tariff, tariffMatrix)
      _ <- checkFreeFunds(sellerAccount, requiredFunds)
    } yield {
      val effects = List(
        ProcessingEffect.ValidatorsReward(fee),
        ProcessingEffect.Withdraw(from, requiredFunds),
        ProcessingEffect.Accrue(tx.user, tx.reward),
        ProcessingEffect.NewOffer(
          domain.Offer(
            seller = from,
            user = tx.user,
            dataRef = tx.dataRef,
            tariff = tx.tariff,
            purchaseCount = 0,
            blockHeight = state.lastBlockHeight
          )
        )
      )
      if (state.accounts.contains(tx.user)) effects
      else ProcessingEffect.NewAccount(newAccount(tx.user)) :: effects
    }

  def applyEffectToState(state: ProcessingState, effect: ProcessingEffect): ProcessingState = {

    import ProcessingEffect._

    def addAccount(address: Address, free: Mytc, frozen: Mytc) = {
      val originalAccount = state
        .accounts
        .getOrElse(address, newAccount(address))
      val updatedAccount = originalAccount
        .copy(
          free = originalAccount.free + free,
          frozen = originalAccount.frozen + frozen
        )
      state.copy(accounts = state.accounts + (address -> updatedAccount))
    }

    def incrementPurchases(ref: DataRef) =
      state.offers.get(ref).fold(state) { offer =>
        val updated = offer.copy(purchaseCount = offer.purchaseCount + 1)
        state.copy(offers = state.offers + (ref -> updated))
      }

    def addDeposit(id: DepositId, amount: Mytc) = {
      val currentDeposit = state.deposits.getOrElse(id, domain.Deposit(id, Mytc.zero, state.lastBlockHeight))
      val updatedDeposit = currentDeposit.copy(amount = currentDeposit.amount + amount, block = state.lastBlockHeight)
      state.copy(deposits = state.deposits + (id -> updatedDeposit))
    }

    effect match {
      case NewAccount(account) => state.copy(accounts = state.accounts + (account.address -> account))
      case NewOffer(offer) => state.copy(offers = state.offers + (offer.dataRef -> offer))
      case Freeze(address, amount) => addAccount(address, Mytc(-amount), amount)
      case Unfreeze(address, amount) => addAccount(address, amount, Mytc(-amount))
      case Accrue(address, amount) => addAccount(address, amount, Mytc.zero)
      case Withdraw(address, amount) => addAccount(address, Mytc(-amount), Mytc.zero)
      case AccrueDeposit(id, amount) => addDeposit(id, amount)
      case WithdrawDeposit(id, amount) => addDeposit(id, Mytc(-amount))
      case IncrementOfferPurchaseCount(dataRef) => incrementPurchases(dataRef)
      case Punish(punishment) => state.copy(punished = state.punished + punishment )
      case Confirm(confirmation) => state.copy(confirmed = state.confirmed + confirmation )
      case UpdateTariffMatrix(tariffMatrix) =>
        state.copy(tariffMatrices = state.tariffMatrices + (tariffMatrix.vendor -> tariffMatrix))
      case SetMultiplier(vendor, value) => state.copy(multipliers = state.multipliers + (vendor -> value))
      // This effect doesn't affect processing state
      case _: CheckDomainAndUpdateInfo => state
      case _: ValidatorsReward => state
    }
  }

  // Helpers

  def mergedMatix(curMatrix: TariffMatrix, prevMatrixOpt: Option[TariffMatrix]): TariffMatrix = {
    prevMatrixOpt match {
      case None => curMatrix
      case Some(prevMatrix) if curMatrix.tariffs >= prevMatrix.tariffs => curMatrix
      case Some(prevMatrix) => {
        val records = math.max(prevMatrix.records, curMatrix.records)
        val tariffs = prevMatrix.tariffs
        val data = (1 to tariffs).toVector.flatMap {
          tid =>
            val values = if (tid <= curMatrix.tariffs) {
              curMatrix.tariff(tid)
            } else {
              prevMatrix.tariff(tid)
            }
            values ++ Vector.fill(records - values.length)(values.last)
        }
        TariffMatrix (
          vendor = curMatrix.vendor,
          tariffs = tariffs,
          records = records,
          data = data
        )
      }
    }
  }

  def newAccount(address: Address) =
    domain.Account(address, Mytc.zero, Mytc.zero)

  def tariffMatrixByAddress(vendor: Address, state: ProcessingState): Either[ProcessingError, TariffMatrix] = {
    state.tariffMatrices.get(vendor).toRight(ProcessingError.TariffMatrixNotFound)
  }

  def multiplierByAddress(address: Address, state: ProcessingState): BigDecimal = {
    state.multipliers.getOrElse(address, BigDecimal(1.0))
  }

  def offerByDataRef(dataRef: DataRef, state: ProcessingState): Either[ProcessingError.OfferNotFound, domain.Offer] =
    state
      .offers
      .get(dataRef)
      .toRight(ProcessingError.OfferNotFound(dataRef))

  def accountByAddress(address: Address, state: ProcessingState): Either[ProcessingError, domain.Account] =
    state
      .accounts
      .get(address)
      .toRight[ProcessingError](ProcessingError.AddressNotFound(address))

  def checkFunds(account: domain.Account, requiredFunds: Mytc, f: domain.Account => BigDecimal): Either[ProcessingError, Unit] =
    Either.cond(
      test = f(account) >= requiredFunds,
      left = ProcessingError.NotEnoughFunds,
      right = ()
    )

  def depositById(id: DepositId, state: ProcessingState): Either[ProcessingError, domain.Deposit] =
    state
      .deposits
      .get(id)
      .toRight[ProcessingError](ProcessingError.DepositNotFound(id))

  def checkDeposit(deposit: domain.Deposit, required: Mytc): Either[ProcessingError, Unit] =
    Either.cond(
      test = deposit.amount >= required,
      left = ProcessingError.NotEnoughFunds,
      right = ()
    )

  def checkIfNotPunished(customer: Address, data: DataRef, state: ProcessingState): Either[ProcessingError, Unit] = {
    Either.cond(
      !state.punished.contains(domain.Punishment(customer, data)),
      (),
      ProcessingError.AlreadyPunished
    )
  }

  def checkIfNotConfirmed(customer: Address, data: DataRef, state: ProcessingState): Either[ProcessingError, Unit] = {
      Either.cond(
        !state.confirmed.contains(domain.Confirmation(customer, data)),
        (),
        ProcessingError.AlreadyConfirmed
      )
  }

  def checkFrozenFunds(account: domain.Account, requiredFunds: Mytc): Either[ProcessingError, Unit] =
    checkFunds(account, requiredFunds, _.frozen)

  def checkFreeFunds(account: domain.Account, requiredFunds: Mytc): Either[ProcessingError, Unit] =
    checkFunds(account, requiredFunds, _.free)

  def validateMatrix(tariffMatrix: TariffMatrix): Either[ProcessingError, Unit] = {
    def sanityCheck(tariffMatrix: TariffMatrix): Boolean = {
      tariffMatrix.tariffs > 0 && tariffMatrix.records > 0 &&
      tariffMatrix.records * tariffMatrix.tariffs == tariffMatrix.matrix.data.length
    }
    checkNotNegative(tariffMatrix.matrix : _*)
      .flatMap( _ =>
        Either.cond (
          test = sanityCheck(tariffMatrix),
          left = ProcessingError.InvalidMatrix,
          right = ()
        )
      )
      .flatMap( _ =>
        Either.cond (
          test = {
            tariffMatrix.records == 1 ||
            (1 to tariffMatrix.tariffs).forall {
                tariffMatrix.tariff(_)
                  .sliding(2)
                  .forall(v => v(0) >= v(1))
              }
            }
          ,
          left = ProcessingError.InvalidMatrix,
          right = ()
        )
      )
  }
  def checkNotNegative(values: BigDecimal*): Either[ProcessingError, Unit] = {
    Either.cond(
      test = values.forall(_ >= 0),
      left = ProcessingError.ShouldNotBeNegative,
      right = ()
    )
  }

  def validateTariff(tariff: Int, tariffMatrix: TariffMatrix): Either[ProcessingError, Unit] = {
    Either.cond[ProcessingError, Unit](
      test = tariff > 0 && tariff <= tariffMatrix.tariffs,
      left = ProcessingError.NoSuchTariff,
      right = ()
    )
  }
  def checkPositive(values: Mytc*): Either[ProcessingError.ShouldBePositive.type, Unit] =
    Either.cond(
      test = values.forall(_ > 0),
      left = ProcessingError.ShouldBePositive,
      right = ()
    )

  def checkIntention(intention: PurchaseIntention): Either[ProcessingError, AuthorizedPurchaseIntention] = {
    cryptography.checkIntention(intention).toRight(left = ProcessingError.InvalidSignature)
  }


  // Processing Data

  type ProcessingResult = Either[ProcessingError, List[ProcessingEffect]]

  case class ProcessingState(offers: Map[DataRef, domain.Offer] = Map.empty,
                             accounts: Map[Address, domain.Account] = Map.empty,
                             companies: Map[Address, domain.Organization] = Map.empty,
                             deposits: Map[DepositId, domain.Deposit] = Map.empty,
                             punished: Set[domain.Punishment] = Set.empty,
                             confirmed: Set[domain.Confirmation] = Set.empty,
                             tariffMatrices: Map[Address, TariffMatrix] = Map.empty,
                             multipliers: Map[Address, BigDecimal] = Map.empty,
                             lastBlockHeight: Long = 0,
                             lastBlockHash: ByteString = ByteString.EMPTY
  )

  sealed trait ProcessingEffect

  object ProcessingEffect {
    // Simple state effects
    case class Accrue(address: Address, amount: Mytc) extends ProcessingEffect
    case class Withdraw(address: Address, amount: Mytc) extends ProcessingEffect
    case class Freeze(address: Address, amount: Mytc) extends ProcessingEffect
    case class Unfreeze(address: Address, amount: Mytc) extends ProcessingEffect
    case class NewAccount(account: domain.Account) extends ProcessingEffect
    case class NewOffer(offer: domain.Offer) extends ProcessingEffect
    case class AccrueDeposit(id: DepositId, amount: Mytc) extends ProcessingEffect
    case class WithdrawDeposit(id: DepositId, amount: Mytc) extends ProcessingEffect
    case class IncrementOfferPurchaseCount(dataRef: DataRef) extends ProcessingEffect
    case class Punish(punishment: domain.Punishment) extends ProcessingEffect
    case class Confirm(confirmation: domain.Confirmation) extends ProcessingEffect
    case class UpdateTariffMatrix(tariffMatrix: TariffMatrix) extends ProcessingEffect
    case class SetMultiplier(address: Address, value: BigDecimal) extends ProcessingEffect
    // Special effects
    case class CheckDomainAndUpdateInfo(domain: String, address: Address) extends ProcessingEffect
    case class ValidatorsReward(amount: Mytc) extends ProcessingEffect
  }

  sealed trait ProcessingError

  object ProcessingError {
    case object InvalidDomain extends ProcessingError
    case object AlreadyConfirmed extends ProcessingError
    case object AlreadyPunished extends ProcessingError
    case object InvalidSignature extends ProcessingError
    case object ShouldBePositive extends ProcessingError
    case object ShouldNotBeNegative extends ProcessingError
    case object NotEnoughFunds extends ProcessingError
    case object WrongEra extends ProcessingError
    case object NoSuchTariff extends ProcessingError
    case object InvalidMatrix extends ProcessingError

    // Not founds
    case class DepositNotFound(id: DepositId) extends ProcessingError
    case class AddressNotFound(address: Address) extends ProcessingError
    case class OfferNotFound(dataRef: DataRef) extends ProcessingError
    case object TariffMatrixNotFound extends ProcessingError

  }
}
