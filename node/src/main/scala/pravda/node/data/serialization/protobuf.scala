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

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.data.blockchain.{Address, NativeCoin}
import supertagged.{Tagged, lifterF}
import zhukov._
import com.google.protobuf.ByteString
import pravda.common.data.blockchain
import pravda.common.data.blockchain.{Address, NativeCoin}
import pravda.common.data.blockchain._
import pravda.common.data.blockchain.Transaction._
import pravda.common.cryptography.EncryptedPrivateKey
import pravda.common.vm.{Effect, MarshalledData}
import pravda.common.serialization.{ProtobufTranscoder, ZhukovLowPriorityInstances}
import pravda.node.data.domain.Wallet
import pravda.node.servers.Abci.AdditionalDataForAddress
import supertagged.{@@, Tagged, lifterF}
import zhukov._
import zhukov.derivation._
import zhukov.Default.auto._

object protobuf extends ProtobufTranscoder with ZhukovInstances

trait ZhukovInstances extends pravda.common.serialization.ZhukovInstances {

  implicit lazy val additionalDataForAddressMarshaller: Marshaller[AdditionalDataForAddress] = marshaller

  implicit lazy val walletMarshaller: Marshaller[Wallet] = marshaller

  implicit lazy val walletUnmarshaller: Unmarshaller[Wallet] = unmarshaller
  implicit lazy val additionalDataForAddressUnmarshaller: Unmarshaller[AdditionalDataForAddress] = unmarshaller

  implicit lazy val walletSizeMeter: SizeMeter[Wallet] = sizeMeter
  implicit lazy val additionalDataForAddressSizeMeter: SizeMeter[AdditionalDataForAddress] = sizeMeter
}
