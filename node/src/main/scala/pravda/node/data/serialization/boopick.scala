package pravda.node.data.serialization

import java.nio.ByteBuffer

import boopickle.Default._
import boopickle.{CompositePickler, Pickler}
import com.google.protobuf.{ByteString => PbByteString}
import pravda.node.data.blockchain.Transaction
import supertagged.{@@, lifterF}
import akka.util.{ByteString => AkkaByteString}

object boopick {

  //---------------------------------------------------------------------------
  // Misc picklers
  //---------------------------------------------------------------------------

  /** This pickler allow to use protocol buffers' ByteString inside case classes */
  implicit val pbByteStringPickler: Pickler[PbByteString] =
    transformPickler[PbByteString, Array[Byte]](byteArray => PbByteString.copyFrom(byteArray))(byteString =>
      byteString.toByteArray)

  /** Allow to use tagged primitives inside case classes */
  implicit def picklerLifter[T: Pickler, U]: Pickler[@@[T, U]] =
    lifterF[Pickler].lift[T, U]

  //---------------------------------------------------------------------------
  // Simple picklers
  //---------------------------------------------------------------------------

  implicit val signedTransactionPickler: Pickler[Transaction.SignedTransaction] =
    generatePickler[Transaction.SignedTransaction]

  implicit val unsignedTransactionPickler: Pickler[Transaction.UnsignedTransaction] =
    generatePickler[Transaction.UnsignedTransaction]

  implicit val authorizedTransactionPickler: Pickler[Transaction.AuthorizedTransaction] =
    generatePickler[Transaction.AuthorizedTransaction]

  //---------------------------------------------------------------------------
  // Composite picklers
  //---------------------------------------------------------------------------

  implicit val transactionPickler: CompositePickler[Transaction] =
    compositePickler[Transaction]
      .addConcreteType[Transaction.SignedTransaction]
      .addConcreteType[Transaction.UnsignedTransaction]
      .addConcreteType[Transaction.AuthorizedTransaction]

  //---------------------------------------------------------------------------
  // Transcoding
  //---------------------------------------------------------------------------

  implicit def binaryEncoder[T: Pickler]: Transcoder[T, BooPickle] = { obj =>
    val buffer = Pickle.intoBytes(obj)
    val bytes = Array.ofDim[Byte](buffer.remaining())
    buffer.get(bytes)
    BooPickle(bytes)
  }

  implicit def bpByteStringEncoder[T: Pickler]: Transcoder[T, PbByteString] = { value =>
    val buffer = Pickle.intoBytes(value)
    PbByteString.copyFrom(buffer)
  }

  implicit def binaryDecoder[T: Pickler]: Transcoder[BooPickle, T] = { binary =>
    val buffer = ByteBuffer.wrap(binary)
    Unpickle[T].fromBytes(buffer)
  }

  implicit def akkaByteStringDecoder[T: Pickler]: Transcoder[AkkaByteString, T] = { binary =>
    val buffer = binary.toByteBuffer
    Unpickle[T].fromBytes(buffer)
  }

  implicit def bpByteStringDecoder[T: Pickler]: Transcoder[PbByteString, T] = { binary =>
    val buffer = ByteBuffer.wrap(binary.toByteArray)
    Unpickle[T].fromBytes(buffer)
  }
}
