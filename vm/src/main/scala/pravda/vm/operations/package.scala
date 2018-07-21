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

package pravda.vm

import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}
import pravda.vm.Data.Primitive._
import pravda.vm.VmError.{InvalidAddress, InvalidCoinAmount, WrongType}

package object operations {

  /**
    * Applies `f` to two top items from stack.
    * Pushes application result to stack.
    * @param f binary operation
    */
  def binaryOperation(memory: Memory, wattCounter: WattCounter)(f: (Data, Data) => Data.Primitive): Unit = {
    val a = memory.pop()
    val b = memory.pop()
    val r = f(a, b)
    wattCounter.memoryUsage(r.volume.toLong)
    memory.push(r)
  }

  def ref(value: Data): Ref = value match {
    case x: Ref => x
    case _      => throw VmErrorException(WrongType)
  }

  def integer(value: Data.Primitive): Long = value match {
    case Int8(x)   => x.toLong
    case Int16(x)  => x.toLong
    case Int32(x)  => x.toLong
    case Uint8(x)  => x.toLong
    case Uint16(x) => x.toLong
    case Uint32(x) => x.toLong
    case BigInt(x) => x.toLong
    case _         => throw VmErrorException(WrongType)
  }

  def boolean(value: Data.Primitive): Boolean = value match {
    case Bool.True  => true
    case Bool.False => false
    case _          => throw VmErrorException(WrongType)
  }

  def bytes(a: Data): ByteString = {
    a match {
      case Bytes(data) => data
      case _           => throw VmErrorException(WrongType)
    }
  }

  def bytes(a: ByteString): Bytes = Bytes(a)

  def coins(a: Data): NativeCoin = a match {
    case BigInt(data) if data < Long.MinValue || data > Long.MaxValue => throw VmErrorException(InvalidCoinAmount)
    case BigInt(data)                                                 => NativeCoin @@ data.toLong
    case _                                                            => throw VmErrorException(WrongType)
  }

  def coins(a: NativeCoin): Data.Primitive =
    BigInt(scala.BigInt(a))

  def address(a: Data): Address = {
    val bytes = a match {
      case Bytes(data) => data
      case _           => throw VmErrorException(WrongType)
    }
    if (bytes.size() == 32) Address @@ bytes
    else throw VmErrorException(InvalidAddress)
  }

  def address(bytes: Address): Data.Primitive = {
    if (bytes.size() == 32) Bytes(bytes)
    else throw VmErrorException(InvalidAddress)
  }
}
