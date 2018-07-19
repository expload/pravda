package pravda.node.data

import java.io.File

import pravda.common.domain.Address
import pravda.node.data.common.CoinDistributionMember
import pravda.node.data.cryptography.PrivateKey

final case class TimechainConfig(genesis: TimechainConfig.Genesis,
                                 paymentWallet: TimechainConfig.PaymentWallet,
                                 isValidator: Boolean,
                                 coinDistribution: Seq[CoinDistributionMember],
                                 dataDirectory: File,
                                 seeds: String,
                                 api: TimechainConfig.ApiConfig,
                                 ui: TimechainConfig.UiConfig,
                                 tendermint: TimechainConfig.TendermintConfig)

object TimechainConfig {
  final case class Genesis(
      time: String,
      chainId: String,
      appHash: String,
      validators: Seq[GenesisValidator],
      distribution: Boolean
  )
  final case class PaymentWallet(
      privateKey: PrivateKey,
      address: Address
  )

  final case class GenesisValidator(
      publicKey: CryptoKey,
      power: Int,
      name: String
  )
  final case class CryptoKey(
      `type`: String,
      data: String
  )
  final case class ApiConfig(
      host: String,
      port: Int
  )
  final case class UiConfig(
      host: String,
      port: Int
  )
  final case class TendermintConfig(
      peerPort: Int,
      rpcPort: Int,
      proxyAppPort: Int,
      useUnixDomainSocket: Boolean
  )
}
