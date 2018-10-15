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

package pravda.vm.operations

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.vm.Data.Array._
import pravda.vm.Data.Primitive.{Bool, _}
import pravda.vm.Data.{Primitive, Struct, Type}
import pravda.vm.Opcodes._
import pravda.vm.Error.WrongType
import pravda.vm._
import pravda.vm.operations.annotation.OpcodeImplementation

import scala.collection.mutable.ArrayBuffer

/**
  * Pravda VM heap pravda.vm.Opcodes implementation.
  * @see pravda.vm.Opcodes
  * @param memory Access to VM memory
  */
final class HeapOperations(memory: Memory, program: ByteBuffer, wattCounter: WattCounter) {

  @OpcodeImplementation(
    opcode = PRIMITIVE_PUT,
    description = "Puts top item from the stack to the memory.Pushes reference to the stack."
  )
  def primitivePut(): Unit = {
    val data = memory.pop()
    val i = memory.heapPut(data)
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(Data.Primitive.Ref(i))
  }

  @OpcodeImplementation(
    opcode = PRIMITIVE_GET,
    description =
      "Uses top item from the stack as referenceto data in the memory of program. Pushesretrieved data to the stack."
  )
  def primitiveGet(): Unit = {
    val i = ref(memory.pop())
    memory.heapGet(i.data) match {
      case primitive: Data.Primitive => memory.push(primitive)
      case _                         => throw ThrowableVmError(Error.WrongType)
    }
  }

  @OpcodeImplementation(
    opcode = NEW,
    description = "Puts the data following the opcode to the heap. " +
      "Pushes reference to the stack."
  )
  def `new`(): Unit = {
    val data = Data.readFromByteBuffer(program)
    val i = memory.heapPut(data)
    val reference = Data.Primitive.Ref(i)
    wattCounter.memoryUsage(data.volume.toLong)
    wattCounter.memoryUsage(reference.volume.toLong)
    memory.push(reference)
  }

  @OpcodeImplementation(
    opcode = NEW_ARRAY,
    description = "Takes type of desired array from the stack. " +
      "Takes length of the desired array from the stack. " +
      "Pushes reference of new array to the stack."
  )
  def newArray(): Unit = {
    val `type` = integer(memory.pop())
    val num = integer(memory.pop())

    val arr = Type @@ `type`.toByte match {
      case Type.Int8    => Data.Array.Int8Array(ArrayBuffer.fill(num.toInt)(0))
      case Type.Int16   => Data.Array.Int16Array(ArrayBuffer.fill(num.toInt)(0))
      case Type.Int32   => Data.Array.Int32Array(ArrayBuffer.fill(num.toInt)(0))
      case Type.Uint8   => Data.Array.Uint8Array(ArrayBuffer.fill(num.toInt)(0))
      case Type.Uint16  => Data.Array.Uint16Array(ArrayBuffer.fill(num.toInt)(0))
      case Type.Uint32  => Data.Array.Uint32Array(ArrayBuffer.fill(num.toInt)(0L))
      case Type.BigInt  => Data.Array.BigIntArray(ArrayBuffer.fill(num.toInt)(scala.BigInt(0)))
      case Type.Number  => Data.Array.NumberArray(ArrayBuffer.fill(num.toInt)(0.0))
      case Type.Ref     => Data.Array.RefArray(ArrayBuffer.fill(num.toInt)(0))
      case Type.Boolean => Data.Array.BoolArray(ArrayBuffer.fill(num.toInt)(Data.Primitive.Bool.False))
      case Type.Utf8    => Data.Array.Utf8Array(ArrayBuffer.fill(num.toInt)(""))
      case Type.Bytes   => Data.Array.BytesArray(ArrayBuffer.fill(num.toInt)(ByteString.EMPTY))
      case _            => throw ThrowableVmError(WrongType)
    }

    memory.push(Data.Primitive.Ref(memory.heapPut(arr)))
  }

  @OpcodeImplementation(
    opcode = ARRAY_GET,
    description = "Takes reference to array and index from the stack." +
      "Pushes to the stack a primitive at index in array corresponding by the given reference."
  )
  def arrayGet(): Unit = {
    val index = integer(memory.pop()).toInt
    memory.pop() match {
      case Ref(reference) =>
        val datum = memory.heapGet(reference) match {
          case Bytes(data)       => Uint8(data.byteAt(index) & 0xFF)
          case Utf8(data)        => Uint8(data.charAt(index).toByte & 0xFF)
          case Int8Array(data)   => Int8(data(index))
          case Int16Array(data)  => Int16(data(index))
          case Int32Array(data)  => Int32(data(index))
          case Uint8Array(data)  => Uint8(data(index))
          case Uint16Array(data) => Uint16(data(index))
          case Uint32Array(data) => Uint32(data(index))
          case BigIntArray(data) => BigInt(data(index))
          case RefArray(data)    => Ref(data(index))
          case BoolArray(data)   => data(index)
          case Utf8Array(data)   => Utf8(data(index))
          case BytesArray(data)  => Bytes(data(index))
          case _                 => throw ThrowableVmError(WrongType)
        }
        wattCounter.memoryUsage(datum.volume.toLong)
        memory.push(datum)
      case Utf8(data) =>
        val datum = Uint8(data.charAt(index).toByte & 0xFF)
        wattCounter.memoryUsage(datum.volume.toLong)
        memory.push(datum)
      case Bytes(data) =>
        val datum = Uint8(data.byteAt(index) & 0xFF)
        wattCounter.memoryUsage(datum.volume.toLong)
        memory.push(datum)
      case _ => throw ThrowableVmError(WrongType)
    }
  }

  @OpcodeImplementation(
    opcode = ARRAY_MUT,
    description = "Takes reference to array, primitive and index from the stack." +
      "Puts a primitive at index in array corresponding by the given reference."
  )
  def arrayMut(): Unit = {
    val index = integer(memory.pop()).toInt
    val primitive = memory.pop()
    val reference = ref(memory.pop())
    val array = memory.heapGet(reference.data)
    (primitive, array) match {
      case (Int8(value), Int8Array(data))     => data(index) = value
      case (Int16(value), Int16Array(data))   => data(index) = value
      case (Int32(value), Int32Array(data))   => data(index) = value
      case (Uint8(value), Uint8Array(data))   => data(index) = value
      case (Uint16(value), Uint16Array(data)) => data(index) = value
      case (Uint32(value), Uint32Array(data)) => data(index) = value
      case (BigInt(value), BigIntArray(data)) => data(index) = value
      case (Ref(value), RefArray(data))       => data(index) = value
      case (value: Bool, BoolArray(data))     => data(index) = value
      case (Utf8(value), Utf8Array(data))     => data(index) = value
      case (Bytes(value), BytesArray(data))   => data(index) = value
      case _                                  => throw ThrowableVmError(WrongType)
    }
  }

  @OpcodeImplementation(
    opcode = LENGTH,
    description =
      "Takes reference to array or Bytes or Utf8 from stack. " +
        "Pushes length of given array, Bytes or Utf8 to the stack. "
  )
  def length(): Unit = {
    val len = memory.pop() match {
      case Ref(reference) =>
        memory.heapGet(reference) match {
          case Int8Array(data)   => data.length
          case Int16Array(data)  => data.length
          case Int32Array(data)  => data.length
          case Uint8Array(data)  => data.length
          case Uint16Array(data) => data.length
          case Uint32Array(data) => data.length
          case BigIntArray(data) => data.length
          case RefArray(data)    => data.length
          case BoolArray(data)   => data.length
          case Utf8Array(data)   => data.length
          case BytesArray(data)  => data.length
          case _                 => throw ThrowableVmError(WrongType)
        }
      case Bytes(data) => data.size
      case Utf8(data)  => data.length
      case _           => throw ThrowableVmError(WrongType)
    }

    memory.push(Data.Primitive.Uint32(len.toLong))
  }

  @OpcodeImplementation(
    opcode = STRUCT_GET,
    description = "Takes reference to struct and key from the stack." +
      "Pushes to the stack a primitive at key in struct corresponding by the given reference."
  )
  def structGet(): Unit = {
    val key = memory.pop()
    val reference = ref(memory.pop())
    val struct = memory.heapGet(reference.data)
    val datum = struct match {
      case Struct(data) =>
        data(key)
      case _ => throw ThrowableVmError(WrongType)
    }
    wattCounter.memoryUsage(datum.volume.toLong)
    memory.push(datum)
  }

  @OpcodeImplementation(
    opcode = STRUCT_GET_STATIC,
    description = "Takes reference to struct from the stack and key from bytes subsequent to opcode." +
      "Pushes to the stack a primitive at key in struct corresponding by the given reference."
  )
  def structGetStatic(): Unit = {
    val reference = ref(memory.pop())
    val key = Data.readFromByteBuffer(program)
    val struct = memory.heapGet(reference.data)
    val datum = (struct, key) match {
      case (Struct(data), k: Primitive) =>
        data(k)
      case _ => throw ThrowableVmError(WrongType)
    }
    wattCounter.memoryUsage(datum.volume.toLong)
    memory.push(datum)
  }

  @OpcodeImplementation(
    opcode = STRUCT_MUT,
    description = "Takes key, primitive and reference to struct from the stack." +
      "Puts a primitive at key in struct corresponding by the given reference."
  )
  def structMut(): Unit = {
    val key = memory.pop()
    val value = memory.pop()
    val reference = ref(memory.pop())
    val struct = memory.heapGet(reference.data)
    struct match {
      case Struct(data) =>
        data(key) = value
      case _ => throw ThrowableVmError(WrongType)
    }
  }

  @OpcodeImplementation(
    opcode = STRUCT_MUT_STATIC,
    description = "Takes primitive and reference to struct from " +
      "the stack and key from bytes subsequent to opcode." +
      "Puts a primitive at key in struct corresponding by the given reference."
  )
  def structMutStatic(): Unit = {
    val value = memory.pop()
    val reference = ref(memory.pop())
    val struct = memory.heapGet(reference.data)
    val key = Data.readFromByteBuffer(program)
    (struct, key) match {
      case (Struct(data), k: Primitive) =>
        data(k) = value
      case _ => throw ThrowableVmError(WrongType)
    }
  }
}
