package pravda.vm.operations

import java.nio.ByteBuffer

import pravda.vm.Data.Array._
import pravda.vm.Data.Primitive.{Bool, _}
import pravda.vm.Data.{Primitive, Struct}
import pravda.vm.VmError.WrongType
import pravda.vm._
import pravda.vm.Opcodes._
import pravda.vm.operations.annotation.OpcodeImplementation

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
      case _                         => throw VmErrorException(VmError.WrongType)
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
          case _                 => throw VmErrorException(WrongType)
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
      case _                                  => throw VmErrorException(WrongType)
    }
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
      case _ => throw VmErrorException(WrongType)
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
      case _ => throw VmErrorException(WrongType)
    }
    wattCounter.memoryUsage(datum.volume.toLong)
    memory.push(datum)
  }

  @OpcodeImplementation(
    opcode = STRUCT_MUT,
    description = "Takes reference to struct, primitive and key from the stack." +
      "Puts a primitive at key in struct corresponding by the given reference."
  )
  def structMut(): Unit = {
    val key = memory.pop()
    val reference = ref(memory.pop())
    val value = memory.pop()
    val struct = memory.heapGet(reference.data)
    struct match {
      case Struct(data) =>
        data(key) = value
      case _ => throw VmErrorException(WrongType)
    }
  }

  @OpcodeImplementation(
    opcode = STRUCT_MUT_STATIC,
    description = "Takes reference to struct and primitive from " +
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
      case _ => throw VmErrorException(WrongType)
    }
  }
}
