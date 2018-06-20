package pravda.vm

import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}
import pravda.vm.state.Data.Array.{Int8Array, Uint8Array}
import pravda.vm.state.Data.Primitive.{BigInt, Bool, Int32, Ref}
import pravda.vm.state.VmError.{InvalidAddress, InvalidCoinAmount, WrongType}
import pravda.vm.state.{Data, Memory, VmErrorException}
import pravda.vm.watt.WattCounter

import scala.collection.mutable

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

  def int32(value: Data): Int = value match {
    case Int32(x) => x
    case _        => throw VmErrorException(WrongType)
  }

  def boolean(value: Data): Boolean = value match {
    case Bool.True  => true
    case Bool.False => false
    case _          => throw VmErrorException(WrongType)
  }

  def bytes(a: Data): ByteString = {
    val bytes = a match {
      case BigInt(data)     => data.toByteArray
      case Uint8Array(data) => data.toArray.map(_.toByte)
      case Int8Array(data)  => data.toArray
      case _                => throw VmErrorException(WrongType)
    }
    ByteString.copyFrom(bytes)
  }

  def bytes(a: ByteString): Int8Array =
    Int8Array(a.toByteArray.to[mutable.Buffer])

  def coin(a: Data): NativeCoin = a match {
    case BigInt(data) if data < Long.MinValue || data > Long.MaxValue => throw VmErrorException(InvalidCoinAmount)
    case BigInt(data)                                                 => NativeCoin @@ data.toLong
    case _                                                            => throw VmErrorException(WrongType)
  }

  def coin(a: NativeCoin): Data =
    BigInt(scala.BigInt(a))

  def address(a: Data): Address = {
    val bytes = a match {
      case BigInt(data)     => data.toByteArray
      case Uint8Array(data) => data.toArray.map(_.toByte)
      case Int8Array(data)  => data.toArray
      case _                => throw VmErrorException(WrongType)
    }
    if (bytes.length == 32) Address @@ ByteString.copyFrom(bytes)
    else throw VmErrorException(InvalidAddress)
  }

  def address(a: Address): Data.Primitive = {
    val bytes = a.toByteArray
    if (bytes.length == 32) BigInt(scala.BigInt(bytes))
    else throw VmErrorException(InvalidAddress)
  }
}
