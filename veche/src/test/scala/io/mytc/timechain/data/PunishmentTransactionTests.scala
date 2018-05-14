package io.mytc.timechain.data

import io.mytc.timechain.data.blockchain.TransactionData
import io.mytc.timechain.data.common.{DataRef, DepositId, Mytc, TariffMatrix}
import io.mytc.timechain.data.domain.{Confirmation, Deposit, Punishment}
import io.mytc.timechain.data.offchain.{PurchaseIntention, PurchaseIntentionData}
import io.mytc.timechain.data.processing.{ProcessingError, ProcessingState}
import io.mytc.timechain.data.testutils.User
import utest._
import testutils._
import testutils.transactions._

object PunishmentTransactionTests extends TestSuite {

  val t = TransactionData

  val initialBob = User.create(free = 0)         // Bob is a vendor
  val initialAlice = User.create(free = 0)       // Alice is a network user
  val bob = User.create(free = 100)         // Bob is a vendor
  val alice = User.create(free = 200)       // Alice is a network user
  val hlestakov = User.create(free = 0)   // Hlestakov is a user data producer

  val hlestakovData = DataRef.fromHex("FFFF")

  val stEmpty = ProcessingState()
  val stStart = ProcessingState(accounts = Map(
    bob.account.address → bob.account,
    alice.account.address → alice.account,
    hlestakov.account.address → hlestakov.account
  ))

  val bobTariffs = TariffMatrix(
      vendor = bob.account.address,
    tariffs = 1,
    records = 1,
    data = Vector(9.0)
  )
  val deposit = Deposit(
    id = DepositId(
      owner = alice.account.address,
      vendor = bob.account.address
    ),
    amount = BigDecimal(12),
    block = 2
  )

  val validState = stStart.copy(
    lastBlockHeight = 4,
    offers = Map(hlestakovData ->
      domain.Offer(
        seller = bob.account.address,
        user = hlestakov.account.address,
        dataRef = hlestakovData,
        tariff = 1,
        purchaseCount = 0,
        blockHeight = 0L
      )
    ),
    deposits = Map(
      deposit.id ->
        deposit
    ),
    tariffMatrices = Map(bobTariffs.vendor -> bobTariffs)
  )
  val intentionData = PurchaseIntentionData(
    nonce = 4,
    address = alice.account.address,
    dataRef = hlestakovData
  )
  val intention = cryptography.signIntention(alice.pkey, intentionData)
  val validPunishment = t.CheatingCustomerPunishment(intention)


  val tests = Tests {
    "valid punishment must be valid" - {
      bob.propose(validPunishment, validState) shouldBeSuccess
    }

    "punishment with wrong signature should not be valid" - {
      val intentionWithWrongSigntature = PurchaseIntention.SignedPurchaseIntention(
        data = intentionData,
        signature = com.google.protobuf.ByteString.copyFromUtf8("qwerty")
      )
      val wrongPunishment = t.CheatingCustomerPunishment(intentionWithWrongSigntature)
      bob.propose(wrongPunishment, validState) shouldBe ProcessingError.InvalidSignature
    }

    "fee in panishment should be positive" - {
      bob.propose(validPunishment, validState, fee = Mytc.zero) shouldBe ProcessingError.ShouldBePositive
      bob.propose(validPunishment, validState, fee = Mytc.amount(-5.0)) shouldBe ProcessingError.ShouldBePositive
    }

    "Bob have to have enough money for punishment" - {
      var poorBobState = validState.copy(accounts = validState.accounts + (bob.account.address -> initialBob.account))
      bob.propose(validPunishment, poorBobState) shouldBe ProcessingError.NotEnoughFunds
    }

    "Deposit must be exist" - {
      var stateWithoutDeposit = validState.copy(deposits = Map())
      bob.propose(validPunishment, stateWithoutDeposit) shouldBe ProcessingError.DepositNotFound(deposit.id)
    }

    "It should be allowed to apply punishment" - {
      val stateWithConfirmed = validState.copy(confirmed = Set(Confirmation(alice.account.address, hlestakovData)))
      bob.propose(validPunishment, stateWithConfirmed) shouldBe ProcessingError.AlreadyConfirmed
      val stateWithPunished = validState.copy(punished = Set(Punishment(alice.account.address, hlestakovData)))
      bob.propose(validPunishment, stateWithPunished) shouldBe ProcessingError.AlreadyPunished
    }

  }

}
