package pravda.node.data

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.node.contrib.ripemd160
import supertagged.TaggedType
import pravda.node.utils._

import scala.util.Try

object common {

  object Mytc extends TaggedType[BigDecimal] {
    val zero = apply(BigDecimal(0))
    def amount(v: Int) = Mytc(BigDecimal(v))
    def amount(v: Double) = Mytc(BigDecimal(v))
    def amount(v: String) = Mytc(BigDecimal(v))
    def fromString(s: String) = Mytc(BigDecimal(s))
  }

  type Mytc = Mytc.Type

  object Address extends TaggedType[ByteString] {

    final val Void = {
      val bytes = ByteString.copyFrom(Array.fill(32)(0.toByte))
      Address(bytes)
    }

    def tryFromHex(hex: String): Try[Address] =
      Try(Address(hex2byteString(hex)))

    def fromHex(hex: String): Address =
      Address(hex2byteString(hex))
  }
  type Address = Address.Type

  /**
    * Ripemd160 hash of BSON representation of signed transaction
    */
  object TransactionId extends TaggedType[ByteString] {

    def forEncodedTransaction(tx: ByteString): TransactionId = {
      // go-wire encoding
      val buffer = ByteBuffer
        .allocate(3 + tx.size)
        .put(0x02.toByte) // size of size
        .putShort(tx.size.toShort) // size
        .put(tx.toByteArray) // data
      val hash = ripemd160.getHash(buffer.array())
      TransactionId @@ ByteString.copyFrom(hash)
    }
  }

  type TransactionId = TransactionId.Type

  final case class ApplicationStateInfo(blockHeight: Long, appHash: ByteString)
}
