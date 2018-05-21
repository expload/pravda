package io.mytc.timechain

import io.mytc.timechain.data.TimechainConfig

import io.mytc.timechain.data.serialization.config._

object Config {
  val timeChainConfig = pureconfig.loadConfigOrThrow[TimechainConfig]("timechain")
}
