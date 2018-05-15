package io.mytc.timechain.data

import io.mytc.timechain.data.common._
import io.mytc.timechain.data.cryptography.EncryptedPrivateKey

object domain {

  final case class Wallet(
      address: Address,
      name: String,
      privateKey: EncryptedPrivateKey
  )

  final case class Account(
      address: Address,
      free: BigDecimal, // do not use mytc cause getquill bug :(
      frozen: BigDecimal
  )

}
