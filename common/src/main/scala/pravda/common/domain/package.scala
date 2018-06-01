package pravda.common

import com.google.protobuf.ByteString
import pravda.common.bytes.hex2byteString
import supertagged.TaggedType

import scala.util.Try

package object domain {

  object NativeCoin extends TaggedType[BigDecimal] {
    val zero = apply(BigDecimal(0))
    def amount(v: Int) = NativeCoin(BigDecimal(v))
    def amount(v: Double) = NativeCoin(BigDecimal(v))
    def amount(v: String) = NativeCoin(BigDecimal(v))
    def fromString(s: String) = NativeCoin(BigDecimal(s))
  }

  type NativeCoin = NativeCoin.Type

  object Address extends TaggedType[ByteString] {

    final val Void = {
      val bytes = ByteString.copyFrom(Array.fill(32)(0.toByte))
      Address(bytes)
    }

    def tryFromHex(hex: String): Try[Address] =
      Try(Address(hex2byteString(hex)))

    def fromHex(hex: String): Address =
      Address(hex2byteString(hex))

    def fromByteArray(arr: Array[Byte]) = {
      Address(ByteString.copyFrom(arr))
    }
  }
  type Address = Address.Type

}
