package pravda.node

import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}
import pravda.node.data.blockchain.Transaction.SignedTransaction
import pravda.node.data.blockchain.TransactionData
import pravda.node.data.common.TransactionId
import pravda.node.servers.Abci.StoredProgram
import pravda.vm.{Data, MarshalledData}
import utest._
import zhukov.SizeMeter

object ProtobufTranscode extends TestSuite {

  import zhukov.derivation._
  import zhukov.Default.auto._
  import pravda.node.data.serialization._
  import pravda.node.data.serialization.protobuf._
  final case class MyData(b: Data)
  implicit lazy val mdM = marshaller[MarshalledData.Simple]
  implicit lazy val mdUm = unmarshaller[MarshalledData.Simple]
  implicit lazy val mdSm = sizeMeter[MarshalledData.Simple]

  val tests = Tests {
    "transcode StoredProgram" - {
      import pravda.node.data.serialization._
      import pravda.node.data.serialization.protobuf._

      val storedProrgram = StoredProgram(ByteString.copyFrom(Array[Byte](0x01, 0x02)), `sealed` = false)
      val protobuf = transcode(storedProrgram).to[Protobuf]
      transcode(protobuf).to[StoredProgram] ==> storedProrgram
    }

    "transcode Address" - {
      import pravda.node.data.serialization._
      import pravda.node.data.serialization.protobuf._

      val addr = Address @@ ByteString.copyFromUtf8("foo")
      val protobuf = transcode(addr).to[Protobuf]
      transcode(protobuf).to[Address] ==> addr
    }

    "transcode NativeCoin" - {
      import pravda.node.data.serialization._
      import pravda.node.data.serialization.protobuf._

      val nc = NativeCoin @@ 100L
      val protobuf = transcode(nc).to[Protobuf]
      transcode(protobuf).to[NativeCoin] ==> nc
    }

    "transcode SignedTransaction" - {
      import pravda.node.data.serialization._
      import pravda.node.data.serialization.protobuf._

      val signedTransaction = SignedTransaction(
        Address @@ ByteString.copyFromUtf8("foo"),
        TransactionData @@ ByteString.copyFromUtf8("bar"),
        ByteString.copyFromUtf8("foobar"),
        100L,
        NativeCoin @@ 5L,
        None,
        None,
        42
      )
      val protobuf = transcode(signedTransaction).to[Protobuf]
      transcode(protobuf).to[SignedTransaction] ==> signedTransaction
    }

    "transcode event data" - {
      import pravda.node.data.serialization._
      import pravda.node.data.serialization.protobuf._

      val eventData: (TransactionId, String, MarshalledData) =
        (TransactionId @@ ByteString.copyFromUtf8("foo"), "bar", MarshalledData.Simple(Data.Primitive.Utf8("foobar")))
      val protobuf = transcode(eventData).to[Protobuf]
      transcode(protobuf).to[(TransactionId, String, MarshalledData)] ==> eventData
    }

    "transcode MarshalledData" - {
      import pravda.node.data.serialization._
      import pravda.node.data.serialization.protobuf._

      val marshalledData: MarshalledData = MarshalledData.Simple(Data.Primitive.Utf8("foobar"))
      val protobuf = transcode(marshalledData).to[Protobuf]

      transcode(protobuf).to[MarshalledData] ==> marshalledData
    }
  }
}
