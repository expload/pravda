package io.mytc.timechain.data

import io.mytc.timechain.data.common.{DepositId, TariffMatrix}
import io.mytc.timechain.data.domain.{Confirmation, Deposit}
import utest._
import io.mytc.timechain.data.{blockchain => blch}

object ProcessingTransactionsTests extends TestSuite {

  import testutils._

  import domain.Offer

  import common.Mytc
  import common.DataRef

  import blch.TransactionData

  import .ProcessingState
  import .ProcessingError
  import .ProcessingEffect

  // ----------------- Users, Data & State ------------------------

  val t = TransactionData

  val trent = User.create()                 // Trent is an arbiter

  val initialBob = User.create(free = 0)         // Bob is a vendor
  val initialAlice = User.create(free = 0)       // Alice is a network user
  val bob = User.create(free = 100)         // Bob is a vendor
  val alice = User.create(free = 200)       // Alice is a network user
  val dostoevsky = User.create(free = 300)  // Dostoevsky sit's aside and looks carefully, maybe he'll write smth on this story
  val hlestakov = User.create(free = 0)   // Hlestakov is a user data producer

  val hlestakovData = DataRef.fromHex("FFFF")
  val bobTariffs = TariffMatrix(
    vendor = bob.account.address,
    tariffs = 1,
    records = 1,
    data = Vector(9.0)
  )

  val stEmpty = ProcessingState()
  val stStart = ProcessingState(accounts = Map(
      bob.account.address → bob.account,
      alice.account.address → alice.account,
      dostoevsky.account.address → dostoevsky.account,
      hlestakov.account.address → hlestakov.account
    ),
    tariffMatrices = Map(bobTariffs.vendor -> bobTariffs)
  )

  // --------------------------------------------------------------

  val tests = Tests {

    "initital distribution tx handled properly" - {
      import ProcessingEffect.Accrue
      import ProcessingEffect.NewAccount
      val investors = List(
        initialAlice.account.address → Mytc.amount(100.0),
        initialBob.account.address → Mytc.amount(200.0)
      )
      val st = stEmpty.copy(lastBlockHeight = 1)
      val tx = t.Distribution(accounts = investors)
      trent.propose(tx, st) match {
        case Left(err)      ⇒ assert(fail"Should not fail")
        case Right(effects) ⇒
          assert(effects.contains(NewAccount(initialAlice.account)))
          assert(effects.contains(NewAccount(initialBob.account)))
          assert(effects.contains(Accrue(initialAlice.account.address, Mytc.amount(100.0))))
          assert(effects.contains(Accrue(initialBob.account.address, Mytc.amount(200.0))))
      }
    }

    "this is me tx emits needed effects and triggers domain check" - {
      import ProcessingEffect.Withdraw
      import ProcessingEffect.ValidatorsReward
      import ProcessingEffect.CheckDomainAndUpdateInfo
      val domain = "alice.demonet.mytc.io"
      val st = stStart
      val tx = t.ThisIsMe(domain)
      alice.propose(tx, st) match {
        case Left(err)      ⇒ assert(fail"Should not fail")
        case Right(effects) ⇒ {
          assert(effects.contains(ValidatorsReward(amount = Mytc.amount(0.5))))
          assert(effects.contains(Withdraw(address = alice.account.address, amount = Mytc.amount(0.5))))
          assert(effects.contains(CheckDomainAndUpdateInfo(domain, alice.account.address)))
          assert(effects.size == 3)
        }
      }
    }

    "transfer changes reciever's and sender's accounts properly" - {
      import ProcessingEffect.Accrue
      import ProcessingEffect.Withdraw
      import ProcessingEffect.ValidatorsReward
      val st = stStart
      val tx = t.Transfer(to = bob.account.address,
                          amount = Mytc.amount(10))
      alice.propose(tx, st, fee = Mytc.amount(0.5)) match {
        case Left(err)      ⇒ assert(fail"Should not fail")
        case Right(effects) ⇒ {
          assert(effects.contains(ValidatorsReward(amount = Mytc.amount(0.5))))
          assert(effects.contains(Accrue(address = bob.account.address, amount = Mytc.amount(10.0))))
          assert(effects.contains(Withdraw(address = alice.account.address, amount = Mytc.amount(10.5))))
          assert(effects.size == 3)
        }
      }
    }

    "new user data tx creates new offer and rewards the user" - {
      import ProcessingEffect.Accrue
      import ProcessingEffect.NewOffer
      import ProcessingEffect.Withdraw
      import ProcessingEffect.ValidatorsReward
      val st = stStart
      val offer = Offer(seller = bob.account.address,
                      dataRef = hlestakovData,
                      user = hlestakov.account.address,
                      tariff = 1,
                      purchaseCount = 0,
                      blockHeight = 0L
      )
      val tx = t.Time(user = hlestakov.account.address,
                      dataRef = hlestakovData,
                      reward = Mytc.amount(1),
                      tariff = 1)
      bob.propose(tx, st, fee = Mytc.amount(0.5)) match {
        case Left(err)      ⇒ assert(fail"Should not fail")
        case Right(effects) ⇒ {
          assert(effects.contains(ValidatorsReward(amount = Mytc.amount(0.5))))
          assert(effects.contains(NewOffer(offer = offer)))
          assert(effects.contains(Accrue(address = hlestakov.account.address, amount = Mytc.amount(1.0))))
          assert(effects.contains(Withdraw(address = bob.account.address, amount = Mytc.amount(1.5))))
          assert(effects.size == 4)
        }
      }
    }

    "buyer can deposit some mytc to vendor" - {
      import ProcessingEffect.Freeze
      import ProcessingEffect.Withdraw
      import ProcessingEffect.ValidatorsReward
      import ProcessingEffect.AccrueDeposit
      val st = stStart
      val tx = t.DataPurchasingDeposit(vendor = bob.account.address,
                      amount = Mytc.amount(10))
      val did = DepositId(alice.account.address, bob.account.address)
      alice.propose(tx, st, fee = Mytc.amount(0.5)) match {
        case Left(err)      ⇒ assert(fail"Should not fail")
        case Right(effects) ⇒ {
          assert(effects.contains(ValidatorsReward(amount = Mytc.amount(0.5))))
          assert(effects.contains(Freeze(address = alice.account.address, amount = Mytc.amount(10))))
          assert(effects.contains(Withdraw(address = alice.account.address, amount = Mytc.amount(0.5))))
          assert(effects.contains(AccrueDeposit(did, amount = Mytc.amount(10))))
          assert(effects.size == 4)
        }
      }
    }

    "purchase confirmation does all the necessary accruings and withdrawals" - {
      import ProcessingEffect.Accrue
      import ProcessingEffect.Confirm
      import ProcessingEffect.Unfreeze
      import ProcessingEffect.Withdraw
      import ProcessingEffect.NewAccount
      import ProcessingEffect.ValidatorsReward
      import ProcessingEffect.IncrementOfferPurchaseCount

      val price = Mytc.amount(9)
      val offer = Offer(
                      seller = bob.account.address,
                      dataRef = hlestakovData,
                      user = hlestakov.account.address,
                      tariff = 1,
                      purchaseCount = 0,
                      blockHeight = 0L
      )
      val did = DepositId(alice.account.address, bob.account.address)
      val st = stStart.copy(
        offers = Map(hlestakovData → offer),
        accounts = Map(alice.account.address → alice.account.copy(frozen = 10)),
        deposits = Map(did -> Deposit(did, 10, 0))
      )
      val txConfirm = t.DataPurchasingConfirmation(dataRef = hlestakovData)
      alice.propose(txConfirm, st, fee = Mytc.amount(0.5)) match {
        case Left(err)      ⇒
          println(err)
          assert(fail"Should not fail")
        case Right(effects) ⇒ {
          assert(effects.contains(ValidatorsReward(amount = Mytc.amount(0.5))))
          assert(effects.contains(IncrementOfferPurchaseCount(dataRef = hlestakovData)))
          assert(effects.contains(Unfreeze(address = alice.account.address, amount = Mytc.amount(9))))
          assert(effects.contains(Withdraw(address = alice.account.address, amount = Mytc.amount(9.5))))
          assert(effects.contains(NewAccount(account = hlestakov.account)))
          // FIXME: Ratio is hardcoded 0.7/0.3
          assert(effects.contains(Accrue(address = bob.account.address, amount = Mytc.amount(6.3))))
          assert(effects.contains(Accrue(address = hlestakov.account.address, amount = Mytc.amount(2.7))))
          assert(effects.contains(Confirm(Confirmation(alice.account.address, hlestakovData))))
          assert(effects.size == 9)
        }
      }
    }

    "no one can transfer mytcs without being registered with blockchain" - {
      import ProcessingError.AddressNotFound
      val st = stEmpty.copy(accounts = Map(bob.account.address → bob.account))
      val tx = t.Transfer(to = bob.account.address,
                          amount = Mytc.amount(10))
      alice.propose(tx, st, fee = Mytc.amount(0.5)) match {
        case Left(err)      ⇒ assert(err == AddressNotFound(alice.account.address))
        case Right(effects) ⇒ assert(fail"Should not succeed")
      }
    }

    "can't do anything to data offer that does not exist" - {
      import ProcessingError.OfferNotFound
      val offer = Offer(seller = bob.account.address,
                      dataRef = hlestakovData,
                      user = hlestakov.account.address,
                      tariff = 1,
                      purchaseCount = 0,
                      blockHeight = 0L
      )
      val st = stStart.copy(offers = Map(),
                            accounts = Map(alice.account.address → alice.account.copy(frozen = 10)))
      val txConfirm = t.DataPurchasingConfirmation(dataRef = hlestakovData)
      alice.propose(txConfirm, st, fee = Mytc.amount(0.5)) match {
        case Left(err)      ⇒ assert(err == OfferNotFound(dataRef = hlestakovData))
        case Right(effects) ⇒ assert(fail"Should not succeed")
      }
    }

    "can't transfer mytcs if insufficient funds" - {
      import ProcessingError.NotEnoughFunds
      val st = stStart
      val tx = t.Transfer(to = bob.account.address,
                          amount = Mytc.amount(1000))
      alice.propose(tx, st, fee = Mytc.amount(0.5)) match {
        case Left(err)      ⇒ assert(err == NotEnoughFunds)
        case Right(effects) ⇒ assert(fail"Should not succeed")
      }
    }

    "can't freeze mytcs if insufficient funds" - {
      import ProcessingError.NotEnoughFunds
      val st = stStart
      val tx = t.DataPurchasingDeposit(vendor = bob.account.address,
                      amount = Mytc.amount(1000))
      alice.propose(tx, st, fee = Mytc.amount(0.5)) match {
        case Left(err)      ⇒ assert(err == NotEnoughFunds)
        case Right(effects) ⇒ assert(fail"Should not succeed")
      }
    }

    "can't confirm purchase if insufficient funds" - {
      import ProcessingError.NotEnoughFunds
      val price = Mytc.amount(11)
      val offer = Offer(seller = bob.account.address,
                      dataRef = hlestakovData,
                      user = hlestakov.account.address,
                      tariff = 1,
                      purchaseCount = 0,
                      blockHeight = 0L
      )
      val did = DepositId(alice.account.address, offer.seller)
      val st = stStart.copy(
        offers = Map(hlestakovData → offer),
        accounts = Map(alice.account.address → alice.account.copy(frozen = 10)),
        deposits = Map(did -> Deposit(did, price, 0)),
        tariffMatrices = Map(bob.account.address -> TariffMatrix(bob.account.address, 1, 1, Vector(price.bigDecimal)))
      )
      val txConfirm = t.DataPurchasingConfirmation(dataRef = hlestakovData)
      alice.propose(txConfirm, st, fee = Mytc.amount(0.5)) match {
        case Left(err)      ⇒ assert(err == NotEnoughFunds)
        case Right(effects) ⇒ assert(fail"Should not succeed")
      }
    }

    "fee must be positive" - {
      import ProcessingError.ShouldBePositive
      val st = stStart
      val tx = t.Transfer(to = bob.account.address,
                          amount = Mytc.amount(100))
      alice.propose(tx, st, fee = Mytc.amount(0.0)) match {
        case Left(err)      ⇒ assert(err == ShouldBePositive)
        case Right(effects) ⇒ assert(fail"Should not succeed")
      }
    }

    "transfer amount must be positive" - {
      import ProcessingError.ShouldBePositive
      val st = stStart
      val tx = t.Transfer(to = bob.account.address,
                          amount = Mytc.amount(0.0))
      alice.propose(tx, st, fee = Mytc.amount(1.0)) match {
        case Left(err)      ⇒ assert(err == ShouldBePositive)
        case Right(effects) ⇒ assert(fail"Should not succeed")
      }
    }

    // TODO: Add WrongEra processing error check

  }

}
