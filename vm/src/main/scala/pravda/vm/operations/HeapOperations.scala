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
import pravda.common.vm.Data.Array._
import pravda.common.vm.Data.Primitive.{Bool, _}
import pravda.common.vm.Data.{Primitive, Struct, Type}
import pravda.common.vm.Opcodes._
import pravda.common.vm.Error.{InvalidArgument, WrongType}
import pravda.vm._
import pravda.common.vm._
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
    description = "Puts the top item from the stack to the memory.Pushes the reference to the stack."
  )
  def primitivePut(): Unit = {
    val data = memory.pop()
    val i = memory.heapPut(data)
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(i)
  }

  @OpcodeImplementation(
    opcode = PRIMITIVE_GET,
    description =
      "Uses the top item from the stack as referenceto to data in the program memory. Pushes retrieved data to the stack."
  )
  def primitiveGet(): Unit = {
    val i = ref(memory.pop())
    memory.heapGet(i) match {
      case primitive: Data.Primitive => memory.push(primitive)
      case _                         => throw ThrowableVmError(Error.WrongType)
    }
  }

  @OpcodeImplementation(
    opcode = NEW,
    description = "Puts the data following the opcode to the heap. " +
      "Pushes the reference to the stack. Refs in structs and ref arrays are prohibited."
  )
  def `new`(): Unit = {
    val data = Data.readFromByteBuffer(program) match {
      case _: Data.Primitive.Ref =>
        throw ThrowableVmError(Error.WrongType)
      case _: Data.Array.RefArray =>
        throw ThrowableVmError(Error.WrongType)
      case data: Data.Struct =>
        // check for refs
        data.data.foreach {
          case (_: Primitive.Ref, _) =>
            throw ThrowableVmError(Error.WrongType)
          case (_, _: Primitive.Ref) =>
            throw ThrowableVmError(Error.WrongType)
          case _ => // do nothing
        }
        data
      case x => x
    }
    val i = memory.heapPut(data)
    wattCounter.memoryUsage(data.volume.toLong)
    wattCounter.memoryUsage(i.volume.toLong)
    memory.push(i)
  }

  @OpcodeImplementation(
    opcode = NEW_ARRAY,
    description = "Takes the type of the desired array from the stack. " +
      "Takes the length of the desired array from the stack. " +
      "Pushes the new array reference to the stack."
  )
  def newArray(): Unit = {
    val `type` = integer(memory.pop())
    val num = integer(memory.pop())

    val arr = Type @@ `type`.toByte match {
      case Type.Int8    => Data.Array.Int8Array(ArrayBuffer.fill(num.toInt)(0))
      case Type.Int16   => Data.Array.Int16Array(ArrayBuffer.fill(num.toInt)(0))
      case Type.Int32   => Data.Array.Int32Array(ArrayBuffer.fill(num.toInt)(0))
      case Type.Int64   => Data.Array.Int64Array(ArrayBuffer.fill(num.toInt)(0L))
      case Type.BigInt  => Data.Array.BigIntArray(ArrayBuffer.fill(num.toInt)(scala.BigInt(0)))
      case Type.Number  => Data.Array.NumberArray(ArrayBuffer.fill(num.toInt)(0.0))
      case Type.Ref     => Data.Array.RefArray(ArrayBuffer.fill(num.toInt)(0))
      case Type.Boolean => Data.Array.BoolArray(ArrayBuffer.fill(num.toInt)(Data.Primitive.Bool.False))
      case Type.Utf8    => Data.Array.Utf8Array(ArrayBuffer.fill(num.toInt)(""))
      case Type.Bytes   => Data.Array.BytesArray(ArrayBuffer.fill(num.toInt)(ByteString.EMPTY))
      case _            => throw ThrowableVmError(WrongType)
    }

    memory.push(memory.heapPut(arr))
  }

  @OpcodeImplementation(
    opcode = ARRAY_GET,
    description = "Takes the reference to the array and the index from the stack." +
      "Pushes to the stack a primitive at index in array corresponding by the given reference."
  )
  def arrayGet(): Unit = {
    val index = integer(memory.pop()).toInt

    def readArray[T](read: Int => T, len: Int): T =
      if (index < 0 || index >= len) {
        throw ThrowableVmError(InvalidArgument)
      } else {
        read(index)
      }

    memory.pop() match {
      case reference: Ref =>
        val datum = memory.heapGet(reference) match {
          case Bytes(data)       => Int8(readArray(data.byteAt, data.size()))
          case Utf8(data)        => Int8(readArray(data.charAt, data.length).toByte)
          case Int8Array(data)   => Int8(readArray(data.apply, data.length))
          case Int16Array(data)  => Int16(readArray(data.apply, data.length))
          case Int32Array(data)  => Int32(readArray(data.apply, data.length))
          case Int64Array(data)  => Int64(readArray(data.apply, data.length))
          case BigIntArray(data) => BigInt(readArray(data.apply, data.length))
          case RefArray(data)    => Ref(readArray(data.apply, data.length))
          case BoolArray(data)   => readArray(data.apply, data.length)
          case Utf8Array(data)   => Utf8(readArray(data.apply, data.length))
          case BytesArray(data)  => Bytes(readArray(data.apply, data.length))
          case _                 => throw ThrowableVmError(WrongType)
        }
        wattCounter.memoryUsage(datum.volume.toLong)
        memory.push(datum)
      case Utf8(data) =>
        val datum = Int8(readArray(data.charAt, data.length).toByte)
        wattCounter.memoryUsage(datum.volume.toLong)
        memory.push(datum)
      case Bytes(data) =>
        val datum = Int8(readArray(data.byteAt, data.size()))
        wattCounter.memoryUsage(datum.volume.toLong)
        memory.push(datum)
      case _ => throw ThrowableVmError(WrongType)
    }
  }

  @OpcodeImplementation(
    opcode = ARRAY_MUT,
    description = "Takes the reference to array, primitive and index from the stack." +
      "Puts a primitive at index in the array corresponding by the given reference."
  )
  def arrayMut(): Unit = {
    val index = integer(memory.pop()).toInt
    val primitive = memory.pop()
    val reference = ref(memory.pop())
    val array = memory.heapGet(reference)

    def writeArray[T](value: T, write: (Int, T) => Unit, len: Int): Unit =
      if (index < 0 || index >= len) {
        throw ThrowableVmError(InvalidArgument)
      } else {
        write(index, value)
      }

    (primitive, array) match {
      case (Int8(value), Int8Array(data))     => writeArray(value, data.update, data.length)
      case (Int16(value), Int16Array(data))   => writeArray(value, data.update, data.length)
      case (Int32(value), Int32Array(data))   => writeArray(value, data.update, data.length)
      case (Int64(value), Int64Array(data))   => writeArray(value, data.update, data.length)
      case (BigInt(value), BigIntArray(data)) => writeArray(value, data.update, data.length)
      case (Ref(value), RefArray(data))       => writeArray(value, data.update, data.length)
      case (value: Bool, BoolArray(data))     => writeArray(value, data.update, data.length)
      case (Utf8(value), Utf8Array(data))     => writeArray(value, data.update, data.length)
      case (Bytes(value), BytesArray(data))   => writeArray(value, data.update, data.length)
      case _                                  => throw ThrowableVmError(WrongType)
    }
  }

  @OpcodeImplementation(
    opcode = LENGTH,
    description =
      "Takes the reference to the array or Bytes or Utf8 from the stack. " +
        "Pushes the length of the given array, Bytes or Utf8 to the stack. "
  )
  def length(): Unit = {
    val len = memory.pop() match {
      case reference: Ref =>
        memory.heapGet(reference) match {
          case Int8Array(data)   => data.length
          case Int16Array(data)  => data.length
          case Int32Array(data)  => data.length
          case Int64Array(data)  => data.length
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

    memory.push(Data.Primitive.Int32(len))
  }

  @OpcodeImplementation(
    opcode = STRUCT_GET,
    description = "Takes the reference to the struct and key from the stack." +
      "Pushes to the stack a primitive at the key in the struct corresponding by the given reference."
  )
  def structGet(): Unit = {
    val key = memory.pop()
    val reference = ref(memory.pop())
    val struct = memory.heapGet(reference)
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
    description = "Takes the reference to the struct from the stack and the key from bytes subsequent to opcode." +
      "Pushes to the stack a primitive at the key in the struct corresponding by the given reference."
  )
  def structGetStatic(): Unit = {
    val reference = ref(memory.pop())
    val key = Data.readFromByteBuffer(program)
    val struct = memory.heapGet(reference)
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
    description = "Takes the key, primitive and reference to the struct from the stack." +
      "Puts a primitive at the key in struct corresponding by the given reference."
  )
  def structMut(): Unit = {
    val key = memory.pop()
    val value = memory.pop()
    val reference = ref(memory.pop())
    val struct = memory.heapGet(reference)
    struct match {
      case Struct(data) =>
        data(key) = value
      case _ => throw ThrowableVmError(WrongType)
    }
  }

  @OpcodeImplementation(
    opcode = STRUCT_MUT_STATIC,
    description = "Takes the primitive and reference to the struct from " +
      "the stack and the key from bytes subsequent to opcode." +
      "Puts a primitive at the key in the struct corresponding by the given reference."
  )
  def structMutStatic(): Unit = {
    val value = memory.pop()
    val reference = ref(memory.pop())
    val struct = memory.heapGet(reference)
    val key = Data.readFromByteBuffer(program)
    (struct, key) match {
      case (Struct(data), k: Primitive) =>
        data(k) = value
      case _ => throw ThrowableVmError(WrongType)
    }
  }
}
