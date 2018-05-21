package pravda.node.data

import pravda.node.data.common._
import pravda.node.data.cryptography.EncryptedPrivateKey

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
