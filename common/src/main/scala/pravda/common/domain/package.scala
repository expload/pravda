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

package pravda.common

import com.google.protobuf.ByteString
import pravda.common.bytes._
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

  def eventKey(address: Address, name: String): String =
    s"${byteString2hex(address)}:$name"

  def eventKeyLength(address: Address, name: String): String =
    s"${byteString2hex(address)}:$name:length"

  def eventKeyOffset(address: Address, name: String, offset: Long): String =
    s"${byteString2hex(address)}:$name:$offset"
}
