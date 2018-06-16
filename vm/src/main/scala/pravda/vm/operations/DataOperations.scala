package pravda.vm.operations

import pravda.vm.state.Data.Array._
import pravda.vm.state.Data.Utf8
import pravda.vm.state.VmError.WrongType
import pravda.vm.state.{Memory, VmErrorException}
import pravda.vm.watt.WattCounter
import pravda.vm.watt.WattCounter.CpuWordOperation

final class DataOperations(memory: Memory, wattCounter: WattCounter) {

  def slice(): Unit = {
    val from = int32(memory.pop())
    val until = int32(memory.pop())
    val a = memory.pop()
    val sliced = a match {
      case Utf8(data)        => Utf8(data.substring(from, until))
      case Int8Array(data)   => Int8Array(data.slice(from, until))
      case Int16Array(data)  => Int16Array(data.slice(from, until))
      case Int32Array(data)  => Int32Array(data.slice(from, until))
      case Uint8Array(data)  => Uint8Array(data.slice(from, until))
      case Uint16Array(data) => Uint16Array(data.slice(from, until))
      case Uint32Array(data) => Uint32Array(data.slice(from, until))
      case NumberArray(data) => NumberArray(data.slice(from, until))
      case BigIntArray(data) => BigIntArray(data.slice(from, until))
      case RefArray(data)    => RefArray(data.slice(from, until))
      case BoolArray(data)   => BoolArray(data.slice(from, until))
      case _                 => throw VmErrorException(WrongType)
    }
    wattCounter.cpuUsage(CpuWordOperation(a))
    wattCounter.memoryUsage(sliced.volume.toLong)
    memory.push(sliced)
  }

  def concat(): Unit = {
    val a = memory.pop()
    val b = memory.pop()
    val data = a match {
      case Utf8(lhs) =>
        b match {
          case Utf8(rhs) => Utf8(lhs + rhs)
          case _         => throw VmErrorException(WrongType)
        }
      case Int8Array(lhs) =>
        b match {
          case Int8Array(rhs) => Int8Array(lhs ++ rhs)
          case _              => throw VmErrorException(WrongType)
        }
      case Int16Array(lhs) =>
        b match {
          case Int16Array(rhs) => Int16Array(lhs ++ rhs)
          case _               => throw VmErrorException(WrongType)
        }
      case Int32Array(lhs) =>
        b match {
          case Int32Array(rhs) => Int32Array(lhs ++ rhs)
          case _               => throw VmErrorException(WrongType)
        }
      case Uint8Array(lhs) =>
        b match {
          case Uint8Array(rhs) => Uint8Array(lhs ++ rhs)
          case _               => throw VmErrorException(WrongType)
        }
      case Uint16Array(lhs) =>
        b match {
          case Uint16Array(rhs) => Uint16Array(lhs ++ rhs)
          case _                => throw VmErrorException(WrongType)
        }
      case Uint32Array(lhs) =>
        b match {
          case Uint32Array(rhs) => Uint32Array(lhs ++ rhs)
          case _                => throw VmErrorException(WrongType)
        }
      case NumberArray(lhs) =>
        b match {
          case NumberArray(rhs) => NumberArray(lhs ++ rhs)
          case _                => throw VmErrorException(WrongType)
        }
      case BigIntArray(lhs) =>
        b match {
          case BigIntArray(rhs) => BigIntArray(lhs ++ rhs)
          case _                => throw VmErrorException(WrongType)
        }
      case RefArray(lhs) =>
        b match {
          case RefArray(rhs) => RefArray(lhs ++ rhs)
          case _             => throw VmErrorException(WrongType)
        }
      case BoolArray(lhs) =>
        b match {
          case BoolArray(rhs) => BoolArray(lhs ++ rhs)
          case _              => throw VmErrorException(WrongType)
        }
      case _ => throw VmErrorException(WrongType)
    }
    wattCounter.cpuUsage(CpuWordOperation(a, b))
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(data)
  }

}
