package pravda.node.data.serialization

import pravda.common.domain.{Address, NativeCoin}
import pravda.node.data.TimechainConfig.{CryptoKey, GenesisValidator}
import pravda.node.data.common.TokenSaleMember
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

  implicit val tokenSaleReader: ConfigReader[Seq[TokenSaleMember]] = {
    ConfigReader[String] map {
      _.split(",").toSeq
        .filter(_.nonEmpty)
        .map { s =>
          val Array(address, amount) = s.split(":")
          TokenSaleMember(
            address = Address.fromHex(address),
            amount = NativeCoin @@ amount.toLong
          )
        }
    }
  }

}
