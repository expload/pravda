/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.node.data.serialization

import pravda.common.data.blockchain._
import pravda.common.{bytes => byteUtils}
import pravda.node.data.PravdaConfig.{CryptoKey, GenesisValidator}
import pravda.common.data.blockchain.CoinDistributionMember
import pravda.node.tendermint
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
          val keyBase64 = byteUtils.hexToBase64(key)
          GenesisValidator(
            name = name,
            power = power,
            publicKey = CryptoKey(tendermint.PubKeyEd25519, keyBase64)
          )
        }
    }
  }

  implicit val tokenSaleReader: ConfigReader[Seq[CoinDistributionMember]] = {
    ConfigReader[String] map {
      _.split(",").toSeq
        .filter(_.nonEmpty)
        .map { s =>
          val Array(address, amount) = s.split(":")
          CoinDistributionMember(
            address = Address.fromHex(address),
            amount = NativeCoin @@ amount.toLong
          )
        }
    }
  }

}
