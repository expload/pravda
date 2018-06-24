package pravda.vm.operations

import java.nio.ByteBuffer

import pravda.vm.Data.Array._
import pravda.vm.Data.Primitive.{Bool, _}
import pravda.vm.Data.{Struct, Utf8}
import pravda.vm.VmError.WrongType
import pravda.vm._

/**
  * Pravda VM heap pravda.vm.Opcodes implementation.
  * @see [[pravda.vm.Opcodes]]
  * @param memory Access to VM memory
  */
final class HeapOperations(memory: Memory, program: ByteBuffer, wattCounter: WattCounter) {

  /**
    * Puts top item from the stack to the memory.
    * Pushes reference to the stack.
    *
    * @see [[pravda.vm.Opcodes.PRIMITIVE_PUT]]
    */
  def primitivePut(): Unit = {
    val data = memory.pop()
    val i = memory.heapPut(data)
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(Data.Primitive.Ref(i))
  }

  /**
    * Uses top item from the stack as reference
    * to data in the memory of program. Pushes
    * retrieved data to the stack.
    * @see [[pravda.vm.Opcodes.PRIMITIVE_GET]]
    */
  def primitiveGet(): Unit = {
    val i = ref(memory.pop())
    memory.heapGet(i.data) match {
      case primitive: Data.Primitive => memory.push(primitive)
      case _                         => throw VmErrorException(VmError.WrongType)
    }
  }

  def `new`(): Unit = {
    val data = Data.readFromByteBuffer(program)
    val i = memory.heapPut(data)
    val reference = Data.Primitive.Ref(i)
    wattCounter.memoryUsage(data.volume.toLong)
    wattCounter.memoryUsage(reference.volume.toLong)
    memory.push(reference)
  }

  def arrayGet(): Unit = {
    val reference = ref(memory.pop())
    val index = integer(memory.pop()).toInt
    val datum = memory.heapGet(reference.data) match {
      case Int8Array(data)   => Int8(data(index))
      case Int16Array(data)  => Int16(data(index))
      case Int32Array(data)  => Int32(data(index))
      case Uint8Array(data)  => Uint8(data(index))
      case Uint16Array(data) => Uint16(data(index))
      case Uint32Array(data) => Uint32(data(index))
      case BigIntArray(data) => BigInt(data(index))
      case RefArray(data)    => Ref(data(index))
      case BoolArray(data)   => data(index)
      case _                 => throw VmErrorException(WrongType)
    }
    wattCounter.memoryUsage(datum.volume.toLong)
    memory.push(datum)
  }

  def arrayMut(): Unit = {
    val reference = ref(memory.pop())
    val index = integer(memory.pop()).toInt
    val primitive = memory.pop()
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
      case _                                  => throw VmErrorException(WrongType)
    }
  }

  def structGet(): Unit = {
    val reference = ref(memory.pop())
    val fieldRef = ref(memory.pop())
    val struct = memory.heapGet(reference.data)
    val field = memory.heapGet(fieldRef.data)
    val datum = (struct, field) match {
      case (Struct(data), Utf8(fieldName)) => data(fieldName)
      case _                               => throw VmErrorException(WrongType)
    }
    wattCounter.memoryUsage(datum.volume.toLong)
    memory.push(datum)
  }

  def structGetStatic(): Unit = {
    val reference = ref(memory.pop())
    val field = Data.readFromByteBuffer(program)
    val struct = memory.heapGet(reference.data)
    val datum = (struct, field) match {
      case (Struct(data), Utf8(fieldName)) => data(fieldName)
      case _                               => throw VmErrorException(WrongType)
    }
    wattCounter.memoryUsage(datum.volume.toLong)
    memory.push(datum)
  }

  def structMut(): Unit = {
    val reference = ref(memory.pop())
    val fieldRef = ref(memory.pop())
    val value = memory.pop()
    val struct = memory.heapGet(reference.data)
    val field = memory.heapGet(fieldRef.data)
    (struct, field) match {
      case (Struct(data), Utf8(fieldName)) => data(fieldName) = value
      case _                               => throw VmErrorException(WrongType)
    }
  }

  def structMutStatic(): Unit = {
    val reference = ref(memory.pop())
    val value = memory.pop()
    val struct = memory.heapGet(reference.data)
    val field = Data.readFromByteBuffer(program)
    (struct, field) match {
      case (Struct(data), Utf8(fieldName)) => data(fieldName) = value
      case _                               => throw VmErrorException(WrongType)
    }
  }
}
