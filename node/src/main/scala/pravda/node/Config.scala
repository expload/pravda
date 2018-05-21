package pravda.node

import pravda.node.data.TimechainConfig

import pravda.node.data.serialization.config._

object Config {
  val timeChainConfig = pureconfig.loadConfigOrThrow[TimechainConfig]("timechain")
}
