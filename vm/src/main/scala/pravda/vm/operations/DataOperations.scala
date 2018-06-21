//package pravda.vm.operations
//
//import pravda.vm.Data.Array._
//import pravda.vm.Data.Utf8
//import pravda.vm.VmError.WrongType
//import pravda.vm.state.{Data, Memory, VmErrorException}
//import pravda.vm.WattCounter
//import pravda.vm.WattCounter.CpuWordOperation
//
//final class DataOperations(memory: Memory, wattCounter: WattCounter) {
//
//  /**
//    * Takes start index, end index and reference to item from the stack.
//    * Gets item by reference from heap.
//    * Makes slice of item and puts result to heap.
//    * Puts reference to result to the stack.
//    * @see [[pravda.vm.Opcodes.SLICE]]
//    */
//  def slice(): Unit = {
//    val from = int32(memory.pop())
//    val until = int32(memory.pop())
//    val aRef = ref(memory.pop())
//    val a = memory.heapGet(aRef.data)
//    val sliced = a match {
//      case Utf8(data)        => Utf8(data.substring(from, until))
//      case Int8Array(data)   => Int8Array(data.slice(from, until))
//      case Int16Array(data)  => Int16Array(data.slice(from, until))
//      case Int32Array(data)  => Int32Array(data.slice(from, until))
//      case Uint8Array(data)  => Uint8Array(data.slice(from, until))
//      case Uint16Array(data) => Uint16Array(data.slice(from, until))
//      case Uint32Array(data) => Uint32Array(data.slice(from, until))
//      case NumberArray(data) => NumberArray(data.slice(from, until))
//      case BigIntArray(data) => BigIntArray(data.slice(from, until))
//      case RefArray(data)    => RefArray(data.slice(from, until))
//      case BoolArray(data)   => BoolArray(data.slice(from, until))
//      case _                 => throw VmErrorException(WrongType)
//    }
//    val idx = memory.heapPut(sliced)
//    val resultRef = Data.Primitive.Ref(idx)
//    wattCounter.cpuUsage(CpuWordOperation(a))
//    wattCounter.memoryUsage(sliced.volume.toLong)
//    wattCounter.memoryUsage(resultRef.volume.toLong)
//    memory.push(resultRef)
//  }
//
//  /**
//    * Takes two references from stack.
//    * Gets corresponding items from heap.
//    * Concatenates them.
//    * Puts result to heap.
//    * Put reference to result to stack
//    * @see [[pravda.vm.Opcodes.CONCAT]]
//    */
//  def concat(): Unit = {
//    val aRef = ref(memory.pop())
//    val bRef = ref(memory.pop())
//    val a = memory.heapGet(aRef.data)
//    val b = memory.heapGet(bRef.data)
//    val data = a match {
//      case Utf8(lhs) =>
//        b match {
//          case Utf8(rhs) => Utf8(lhs + rhs)
//          case _         => throw VmErrorException(WrongType)
//        }
//      case Int8Array(lhs) =>
//        b match {
//          case Int8Array(rhs) => Int8Array(lhs ++ rhs)
//          case _              => throw VmErrorException(WrongType)
//        }
//      case Int16Array(lhs) =>
//        b match {
//          case Int16Array(rhs) => Int16Array(lhs ++ rhs)
//          case _               => throw VmErrorException(WrongType)
//        }
//      case Int32Array(lhs) =>
//        b match {
//          case Int32Array(rhs) => Int32Array(lhs ++ rhs)
//          case _               => throw VmErrorException(WrongType)
//        }
//      case Uint8Array(lhs) =>
//        b match {
//          case Uint8Array(rhs) => Uint8Array(lhs ++ rhs)
//          case _               => throw VmErrorException(WrongType)
//        }
//      case Uint16Array(lhs) =>
//        b match {
//          case Uint16Array(rhs) => Uint16Array(lhs ++ rhs)
//          case _                => throw VmErrorException(WrongType)
//        }
//      case Uint32Array(lhs) =>
//        b match {
//          case Uint32Array(rhs) => Uint32Array(lhs ++ rhs)
//          case _                => throw VmErrorException(WrongType)
//        }
//      case NumberArray(lhs) =>
//        b match {
//          case NumberArray(rhs) => NumberArray(lhs ++ rhs)
//          case _                => throw VmErrorException(WrongType)
//        }
//      case BigIntArray(lhs) =>
//        b match {
//          case BigIntArray(rhs) => BigIntArray(lhs ++ rhs)
//          case _                => throw VmErrorException(WrongType)
//        }
//      case RefArray(lhs) =>
//        b match {
//          case RefArray(rhs) => RefArray(lhs ++ rhs)
//          case _             => throw VmErrorException(WrongType)
//        }
//      case BoolArray(lhs) =>
//        b match {
//          case BoolArray(rhs) => BoolArray(lhs ++ rhs)
//          case _              => throw VmErrorException(WrongType)
//        }
//      case _ => throw VmErrorException(WrongType)
//    }
//    val idx = memory.heapPut(data)
//    val newRef = Data.Primitive.Ref(idx)
//    wattCounter.cpuUsage(CpuWordOperation(a, b))
//    wattCounter.memoryUsage(data.volume.toLong)
//    wattCounter.memoryUsage(newRef.volume.toLong)
//    memory.push(newRef)
//  }
//
//}
