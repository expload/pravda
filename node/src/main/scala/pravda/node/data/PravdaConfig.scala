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

package pravda.node.data

import java.io.File

import pravda.common.domain.Address
import pravda.node.data.common.CoinDistributionMember
import pravda.node.data.cryptography.PrivateKey

final case class PravdaConfig(networkAddressCache: Option[PravdaConfig.NetworkAddressCache],
                              genesis: PravdaConfig.Genesis,
                              validatorManager: Option[Address],
                              validator: Option[PravdaConfig.Validator],
                              coinDistribution: Seq[CoinDistributionMember],
                              dataDirectory: File,
                              seeds: String,
                              http: PravdaConfig.HttpConfig,
                              tendermint: PravdaConfig.TendermintConfig)

object PravdaConfig {
  final case class NetworkAddressCache(
      ttl: Int,
      negativeTtl: Int
  )
  final case class Genesis(
      time: String,
      chainId: String,
      appHash: String,
      validators: Seq[GenesisValidator]
  )
  final case class Validator(
      privateKey: PrivateKey,
      address: Address
  )

  final case class GenesisValidator(
      publicKey: CryptoKey,
      power: String,
      name: String
  )
  final case class CryptoKey(
      `type`: String,
      value: String
  )
  final case class HttpConfig(
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
