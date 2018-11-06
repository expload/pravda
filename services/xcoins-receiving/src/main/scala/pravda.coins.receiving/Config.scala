package pravda.coins.receiving

final case class CoinsReceivingConfig(host: String,
                                      port: Int,
                                      testnetEndpoint: String,
                                      walletAddress: String,
                                      walletPrivateKey: String)

object Config {
  val coinsReceivingConfig = pureconfig.loadConfigOrThrow[CoinsReceivingConfig]("receiving")
}
