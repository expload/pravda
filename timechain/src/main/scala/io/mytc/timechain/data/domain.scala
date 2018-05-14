package io.mytc.timechain.data

import io.mytc.timechain.data.common._
import io.mytc.timechain.data.cryptography.EncryptedPrivateKey
import io.mytc.timechain.data.offchain.PurchaseIntention.{AuthorizedPurchaseIntention, SignedPurchaseIntention}
import io.mytc.timechain.data.serialization.Json

object domain {

  case class Wallet(
    address: Address,
    name: String,
    privateKey: EncryptedPrivateKey
  )
  
  case class Account(
    address: Address,
    free: BigDecimal, // do not use mytc cause getquill bug :(
    frozen: BigDecimal
  )

  case class TariffDescription(
    id: Int,
    name: String
  )

  case class Organization(
    address: Address,
    domain: String
  )

  case class Multiplier(
     vendor: Address,
     value: BigDecimal
  )

  case class Offer(
    seller: Address,
    user: Address,
    dataRef: DataRef,
    tariff: Int,
    purchaseCount: Long,
    blockHeight: Long
  )

  case class ActionRecord(
    id: Long,
    address: Address,
    action: Action,
    timestamp: Long,
    timezone: String,
    pending: Boolean
  )

  /**
    * @param dataRef hash of [[jsonData]]
    * @param jsonData json representation of [[Action.ActionsFile]]
    */
  case class ActionPackage(
    dataRef: DataRef,
    userAddress: Address,
    firstRecord: Long,
    lastRecord: Long,
    jsonData: Json
  )

  case class PurchaseIntentionEvidence(
    id: Long,
    dataRef: DataRef,
    address: Address,
    timestamp: Long,
    intention: SignedPurchaseIntention,
    repaid: Boolean
  )

  case class Deposit(
    id: DepositId,
    amount: BigDecimal,
    block: Long
  )

  case class Punishment(
    customer: Address,
    dataRef: DataRef
  )

  case class Confirmation(
    customer: Address,
    dataRef: DataRef
  )

}
