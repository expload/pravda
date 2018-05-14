package io.mytc.timechain.data

import io.mytc.timechain.data.common.TariffMatrix
import io.mytc.timechain.data.domain.Punishment
import io.mytc.timechain.data.processing.ProcessingEffect.{SetMultiplier, UpdateTariffMatrix}
import io.mytc.timechain.data.processing.ProcessingError.TariffMatrixNotFound
import utest._
import io.mytc.timechain.data.{blockchain => blch}
import io.mytc.timechain.data.{processing => proc}
import io.mytc.timechain.data.{cryptography => crypto}

object ProcessingStateEffectsTests extends TestSuite {

  import testutils._

  import domain.Offer

  import common.Mytc
  import common.DataRef

  import proc.ProcessingState
  import proc.ProcessingEffect
  import proc.applyEffectToState

  // ----------------- Users, Data & State ------------------------

  val bob = User.create(free = 100)         // Bob is a vendor
  val alice = User.create(free = 200)       // Alice is a network user
  val hlestakov = User.create(free = 400)   // Hlestakov is a user data producer

  val hlestakovData = DataRef.fromHex("FFFF")
  val stEmpty = ProcessingState()

  // --------------------------------------------------------------

  val tests = Tests {

    "accrue_effect_properly_changes_account's_balance" - {
      import ProcessingEffect.Accrue
      val curSt = stEmpty.copy(accounts = Map(alice.account.address → alice.account.copy(free = 300)))
      val eff = Accrue(address = alice.account.address, amount = Mytc.amount(1))
      val newSt = applyEffectToState(curSt, eff)
      val expSt = stEmpty.copy(accounts = Map(alice.account.address → alice.account.copy(free = 301)))
      assert(newSt == expSt)
    }

    "withdraw_effect_properly_changes_account's_balance" - {
      import ProcessingEffect.Withdraw
      val curSt = stEmpty.copy(accounts = Map(alice.account.address → alice.account.copy(free = 300)))
      val eff = Withdraw(address = alice.account.address, amount = Mytc.amount(1))
      val newSt = applyEffectToState(curSt, eff)
      val expSt = stEmpty.copy(accounts = Map(alice.account.address → alice.account.copy(free = 299)))
      assert(newSt == expSt)
    }

    "free_effect_freezes_given_amount_of_mytcs" - {
      import ProcessingEffect.Freeze
      val curSt = stEmpty.copy(accounts = Map(alice.account.address → alice.account.copy(free = 100, frozen = 0)))
      val eff = Freeze(address = alice.account.address, amount = Mytc.amount(1))
      val newSt = applyEffectToState(curSt, eff)
      val expSt = stEmpty.copy(accounts = Map(alice.account.address → alice.account.copy(free = 99, frozen = 1)))
      assert(newSt == expSt)
    }

    "unfreeze_effect_frees_up_given_amount_of_mytcs_out_of_frozen" - {
      import ProcessingEffect.Unfreeze
      val curSt = stEmpty.copy(accounts = Map(alice.account.address → alice.account.copy(free = 99, frozen = 1)))
      val eff = Unfreeze(address = alice.account.address, amount = Mytc.amount(1))
      val newSt = applyEffectToState(curSt, eff)
      val expSt = stEmpty.copy(accounts = Map(alice.account.address → alice.account.copy(free = 100, frozen = 0)))
      assert(newSt == expSt)
    }

    "new_account_effect_does_create_a_new_account" - {
      import ProcessingEffect.NewAccount
      val curSt = stEmpty
      val eff = NewAccount(account = alice.account)
      val newSt = applyEffectToState(curSt, eff)
      val expSt = stEmpty.copy(accounts = Map(alice.account.address → alice.account))
      assert(newSt == expSt)
    }

    "new_offer_effect_registers_new_effect_with_offers_registry_in_state" - {
      import ProcessingEffect.NewOffer
      val offer = Offer(seller = bob.account.address,
                      dataRef = hlestakovData,
                      user = hlestakov.account.address,
                      tariff = 1,
                      purchaseCount = 0,
                      blockHeight = 0L
      )
      val curSt = stEmpty
      val eff = NewOffer(offer = offer)
      val newSt = applyEffectToState(curSt, eff)
      val expSt = stEmpty.copy(offers = Map(hlestakovData → offer))
      assert(newSt == expSt)
    }

    "there_is_an_effect_to_increment_an_offer_purchase_count" - {
      import ProcessingEffect.IncrementOfferPurchaseCount
      val offer = Offer(seller = bob.account.address,
                      dataRef = hlestakovData,
                      user = hlestakov.account.address,
                      tariff = 1,
                      purchaseCount = 0,
                      blockHeight = 0L
      )
      val curSt = stEmpty.copy(offers = Map(hlestakovData → offer.copy(purchaseCount = 0)))
      val eff = IncrementOfferPurchaseCount(dataRef = hlestakovData)
      val newSt = applyEffectToState(curSt, eff)
      val expSt = stEmpty.copy(offers = Map(hlestakovData → offer.copy(purchaseCount = 1)))
      assert(newSt == expSt)
    }

    "domain_check_effect_does_nothing_to_state" - {
      import ProcessingEffect.CheckDomainAndUpdateInfo
      val curSt = stEmpty
      val eff = CheckDomainAndUpdateInfo(domain = "alice.dev.mytc.io", address = alice.account.address)
      val newSt = applyEffectToState(curSt, eff)
      val expSt = stEmpty
      assert(newSt == expSt)
    }

    "validators_effect_does_nothing_to_state" - {
      import ProcessingEffect.ValidatorsReward
      val curSt = stEmpty
      val eff = ValidatorsReward(amount = Mytc.amount(0.5))
      val newSt = applyEffectToState(curSt, eff)
      val expSt = stEmpty
      assert(newSt == expSt)
    }

    "deny punish effect adds one item to set" - {
      import ProcessingEffect.Punish
      val state = stEmpty
      val effect = Punish(Punishment(alice.account.address, hlestakovData))
      val newState = applyEffectToState(state, effect)
      val expectedState = state.copy(punished = Set(Punishment(alice.account.address, hlestakovData)))
      assert(newState == expectedState)
    }

    "tariff matrix update adds tariffMatrix to the state" - {
      val state = stEmpty
      val matrix = TariffMatrix(bob.account.address)
      val effect = UpdateTariffMatrix(matrix)
      val newState = applyEffectToState(state, effect)
      val expectedState = state.copy(tariffMatrices = Map(matrix.vendor -> matrix))
      assert(newState == expectedState)
    }

    "set multiplier adds multiplier to the state" - {
      val state = stEmpty
      val effect = SetMultiplier(bob.account.address, 3)
      val newState = applyEffectToState(state, effect)
      val expectedState = state.copy(multipliers = Map(bob.account.address -> 3))
      assert(newState == expectedState)
    }

  }

}
