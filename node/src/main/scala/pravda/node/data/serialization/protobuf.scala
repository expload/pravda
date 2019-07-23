package pravda.node.data.serialization

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}
import supertagged.{Tagged, lifterF}
import zhukov._
import com.google.protobuf.ByteString
import pravda.common.domain
import pravda.common.domain.{Address, NativeCoin}
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
