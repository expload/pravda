package io.mytc.timechain.data

import io.mytc.timechain.data.common._
import io.mytc.timechain.data.cryptography.EncryptedPrivateKey

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

}
