package pravda.node.data.serialization

import pravda.node.data.TimechainConfig.{CryptoKey, GenesisValidator}
import pravda.node.data.common.Address
import pravda.node.data.cryptography.PrivateKey
import pureconfig.ConfigReader

/**
  * Implicits custom readers for PureConfig
  */
object config {

  implicit val privateKeyReader: ConfigReader[PrivateKey] = {
    ConfigReader[String] map { s =>
      PrivateKey.fromHex(s)
    }
  }

  implicit val addressReader: ConfigReader[Address] = {
    ConfigReader[String] map { s =>
      Address.fromHex(s)
    }
  }

  implicit val genesisValidatorsReader: ConfigReader[Seq[GenesisValidator]] = {
    ConfigReader[String] map {
      _.split(",").toSeq
        .filter(_.nonEmpty)
        .map { s =>
          val Array(name, power, key) = s.split(":")
          GenesisValidator(
            name = name,
            power = power.toInt,
            publicKey = CryptoKey("ed25519", key)
          )
        }
    }
  }

}
