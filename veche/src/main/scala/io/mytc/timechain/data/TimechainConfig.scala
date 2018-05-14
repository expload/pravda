package io.mytc.timechain.data

import java.io.File

import io.mytc.timechain.data.common.Address
import io.mytc.timechain.data.cryptography.PrivateKey

case class TimechainConfig(
  genesis: TimechainConfig.Genesis,
  paymentWallet: TimechainConfig.PaymentWallet,
  isValidator: Boolean,
  dataDirectory: File,
  seeds: String,
  api: TimechainConfig.ApiConfig,
  tendermint: TimechainConfig.TendermintConfig
)

object TimechainConfig {
  case class Genesis(
    time: String,
    chainId: String,
    appHash: String,
    validators: Seq[GenesisValidator],
    distribution: Boolean
  )
  case class PaymentWallet(
    privateKey: PrivateKey,
    address: Address
  )

  case class GenesisValidator(
    publicKey: CryptoKey,
    power: Int,
    name: String
  )
  case class CryptoKey(
    `type`: String,
    data: String
  )
  case class ApiConfig(
    host: String,
    port: Int
  )
  case class TendermintConfig(
    peerPort: Int,
    rpcPort: Int,
    proxyAppSock: String,
    proxyAppPort: Int
  )


}
