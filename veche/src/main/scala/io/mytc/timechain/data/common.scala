package io.mytc.timechain.data

import com.google.protobuf.ByteString
import io.mytc.timechain.contrib.ripemd160
import supertagged.TaggedType
import io.mytc.timechain.utils._

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

    def forEncodedTransaction(tx: ByteString): TransactionId =
      TransactionId @@ ByteString.copyFrom(ripemd160.getHash(tx.toByteArray))
  }
  type TransactionId = TransactionId.Type

  final case class ApplicationStateInfo(blockHeight: Long, appHash: ByteString)
}
