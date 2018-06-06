package pravda.common

import com.google.protobuf.ByteString
import pravda.common.bytes.hex2byteString
import supertagged.TaggedType

import scala.util.Try

package object domain {

  object NativeCoin extends TaggedType[Long] {
    val zero = apply(0L)
    def amount(v: Int) = NativeCoin(v.toLong)
    def amount(v: String) = NativeCoin(v.toLong)
    def fromString(s: String) = amount(s)
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

    def fromByteArray(arr: Array[Byte]): Address = {
      Address(ByteString.copyFrom(arr))
    }
  }
  type Address = Address.Type

}
