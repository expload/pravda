package pravda.vm.operations

import pravda.vm.VmError.WrongType
import pravda.vm.WattCounter.CpuSimpleArithmetic
import pravda.vm.{Data, Memory, VmErrorException, WattCounter}

import scala.annotation.strictfp

/**
  * Pravda VM logical pravda.vm.Opcodes implementation.
  * @see pravda.vm.Opcodes
  * @param memory Access to VM memory
  * @param wattCounter CPU, memory, storage usage counter
  */
@strictfp final class LogicalOperations(memory: Memory, wattCounter: WattCounter) {

  import Data._
  import Primitive._

  /**
    * Logical NOT (negation).
    * Pops items from stack.
    * If it's 'true' pushes 'false' to stack.
    * Its it's 'false' pushes 'true' to stack.
    * @see pravda.vm.Opcodes.NOT
    */
  def not(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    val x = memory.pop()
    val r = x match {
      case Bool.True  => Bool.False
      case Bool.False => Bool.True
      case _          => throw VmErrorException(WrongType)
    }
    wattCounter.memoryUsage(r.volume.toLong)
    memory.push(r)
  }

  /**
    * Makes 'and' operation on two items from stack.
    * Pushes result to stack.
    * @see pravda.vm.Opcodes.AND
    */
  def and(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    binaryOperation(memory, wattCounter) { (a, b) =>
      a match {
        case Bool.False => Bool.False
        case Bool.True =>
          b match {
            case Bool.True  => Bool.True
            case Bool.False => Bool.False
            case _          => throw VmErrorException(WrongType)
          }
        case _ => throw VmErrorException(WrongType)
      }
    }
  }

  /**
    * Makes 'or' operation on two items from stack.
    * Pushes result to stack.
    * @see pravda.vm.Opcodes.OR
    */
  def or(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    binaryOperation(memory, wattCounter) { (a, b) =>
      a match {
        case Bool.True => Bool.True
        case Bool.False =>
          b match {
            case Bool.True  => Bool.True
            case Bool.False => Bool.False
            case _          => throw VmErrorException(WrongType)
          }
        case _ => throw VmErrorException(WrongType)
      }
    }
  }

  /**
    * Makes 'xor' operation on two items from stack.
    * Pushes result to stack.
    * @see pravda.vm.Opcodes.XOR
    */
  def xor(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    binaryOperation(memory, wattCounter) { (a, b) =>
      a match {
        case Int32(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs ^ rhs)
            case Int16(rhs)  => Int32(lhs ^ rhs)
            case Int32(rhs)  => Int32(lhs ^ rhs)
            case Uint8(rhs)  => Int32(lhs ^ rhs)
            case Uint16(rhs) => Int32(lhs ^ rhs)
            case Uint32(rhs) => Int32((lhs ^ rhs).toInt)
            case BigInt(rhs) => Int32((lhs ^ rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Int16(lhs) =>
          b match {
            case Int8(rhs)   => Int16((lhs ^ rhs).toShort)
            case Int16(rhs)  => Int16((lhs ^ rhs).toShort)
            case Int32(rhs)  => Int16((lhs ^ rhs).toShort)
            case Uint8(rhs)  => Int16((lhs ^ rhs).toShort)
            case Uint16(rhs) => Int16((lhs ^ rhs).toShort)
            case Uint32(rhs) => Int16((lhs ^ rhs).toShort)
            case BigInt(rhs) => Int16((lhs.toInt ^ rhs).toShort)
            case _           => throw VmErrorException(WrongType)
          }
        case Int8(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs ^ rhs)
            case Int16(rhs)  => Int32(lhs ^ rhs)
            case Int32(rhs)  => Int32(lhs ^ rhs)
            case Uint8(rhs)  => Int32(lhs ^ rhs)
            case Uint16(rhs) => Int32(lhs ^ rhs)
            case Uint32(rhs) => BigInt(lhs ^ rhs)
            case BigInt(rhs) => BigInt(lhs.toInt ^ rhs)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint8(lhs) =>
          b match {
            case Int8(rhs)   => Uint8(lhs ^ rhs)
            case Int16(rhs)  => Uint8(lhs ^ rhs)
            case Int32(rhs)  => Uint8(lhs ^ rhs)
            case Uint8(rhs)  => Uint8(lhs ^ rhs)
            case Uint16(rhs) => Uint8(lhs ^ rhs)
            case Uint32(rhs) => Uint8((lhs ^ rhs).toInt)
            case BigInt(rhs) => Uint8((lhs ^ rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint16(lhs) =>
          b match {
            case Int8(rhs)   => Uint16(lhs ^ rhs)
            case Int16(rhs)  => Uint16(lhs ^ rhs)
            case Int32(rhs)  => Uint16(lhs ^ rhs)
            case Uint8(rhs)  => Uint16(lhs ^ rhs)
            case Uint16(rhs) => Uint16(lhs ^ rhs)
            case Uint32(rhs) => Uint16((lhs ^ rhs).toInt)
            case BigInt(rhs) => Uint16((lhs ^ rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint32(lhs) =>
          b match {
            case Int8(rhs)   => Uint32(lhs ^ rhs)
            case Int16(rhs)  => Uint32(lhs ^ rhs)
            case Int32(rhs)  => Uint32(lhs ^ rhs)
            case Uint8(rhs)  => Uint32(lhs ^ rhs)
            case Uint16(rhs) => Uint32(lhs ^ rhs)
            case Uint32(rhs) => Uint32(lhs ^ rhs)
            case BigInt(rhs) => Uint32((lhs ^ rhs).toLong)
            case _           => throw VmErrorException(WrongType)
          }
        case BigInt(lhs) =>
          b match {
            case Int8(rhs)   => BigInt(lhs ^ scala.BigInt(rhs.toInt))
            case Int16(rhs)  => BigInt(lhs ^ scala.BigInt(rhs.toInt))
            case Int32(rhs)  => BigInt(lhs ^ rhs)
            case Uint8(rhs)  => BigInt(lhs ^ rhs)
            case Uint16(rhs) => BigInt(lhs ^ rhs)
            case Uint32(rhs) => BigInt(lhs ^ rhs)
            case BigInt(rhs) => BigInt(lhs ^ rhs)
            case _           => throw VmErrorException(WrongType)
          }
        case _ => throw VmErrorException(WrongType)
      }
    }
  }

  /**
    * Checks top stack item is equal to subsequent stack item.
    * Pushes Bool result to stack.
    * @see pravda.vm.Opcodes.EQ
    */
  def eq(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    binaryOperation(memory, wattCounter)((a, b) => Bool(a == b))
  }

  /**
    * Checks top stack item is greater than subsequent stack item.
    * Pushes Bool result to stack.
    * @see pravda.vm.Opcodes.GT
    */
  def gt(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    binaryOperation(memory, wattCounter)(gtImpl)
  }

  /**
    * Checks top stack item is less than subsequent stack item.
    * Pushes Bool result to stack.
    * @see pravda.vm.Opcodes.LT
    */
  def lt(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    binaryOperation(memory, wattCounter)(ltImpl)
  }

  //---------------------------------------------------------------------
  // Implementations
  //---------------------------------------------------------------------

  private val gtImpl: (Data, Data) => Bool = { (a, b) =>
    val result = a match {
      case Int32(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs
          case Int16(rhs)  => lhs > rhs
          case Int32(rhs)  => lhs > rhs
          case Uint8(rhs)  => lhs > rhs
          case Uint16(rhs) => lhs > rhs
          case Uint32(rhs) => lhs > rhs
          case BigInt(rhs) => lhs > rhs
          case Number(rhs) => lhs > rhs
          case _           => throw VmErrorException(WrongType)
        }
      case Int16(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs
          case Int16(rhs)  => lhs > rhs
          case Int32(rhs)  => lhs > rhs
          case Uint8(rhs)  => lhs > rhs
          case Uint16(rhs) => lhs > rhs
          case Uint32(rhs) => lhs > rhs
          case BigInt(rhs) => rhs < lhs.toInt
          case Number(rhs) => lhs > rhs
          case _           => throw VmErrorException(WrongType)
        }
      case Int8(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs
          case Int16(rhs)  => lhs > rhs
          case Int32(rhs)  => lhs > rhs
          case Uint8(rhs)  => lhs > rhs
          case Uint16(rhs) => lhs > rhs
          case Uint32(rhs) => lhs > rhs
          case BigInt(rhs) => rhs < lhs.toInt
          case Number(rhs) => lhs > rhs
          case _           => throw VmErrorException(WrongType)
        }
      case Uint8(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs
          case Int16(rhs)  => lhs > rhs
          case Int32(rhs)  => lhs > rhs
          case Uint8(rhs)  => lhs > rhs
          case Uint16(rhs) => lhs > rhs
          case Uint32(rhs) => lhs > rhs
          case BigInt(rhs) => lhs > rhs
          case Number(rhs) => lhs > rhs
          case _           => throw VmErrorException(WrongType)
        }
      case Uint16(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs
          case Int16(rhs)  => lhs > rhs
          case Int32(rhs)  => lhs > rhs
          case Uint8(rhs)  => lhs > rhs
          case Uint16(rhs) => lhs > rhs
          case Uint32(rhs) => lhs > rhs
          case BigInt(rhs) => lhs > rhs
          case Number(rhs) => lhs > rhs
          case _           => throw VmErrorException(WrongType)
        }
      case Uint32(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs
          case Int16(rhs)  => lhs > rhs
          case Int32(rhs)  => lhs > rhs
          case Uint8(rhs)  => lhs > rhs
          case Uint16(rhs) => lhs > rhs
          case Uint32(rhs) => lhs > rhs
          case BigInt(rhs) => lhs > rhs
          case Number(rhs) => lhs > rhs
          case _           => throw VmErrorException(WrongType)
        }
      case Number(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs
          case Int16(rhs)  => lhs > rhs
          case Int32(rhs)  => lhs > rhs
          case Uint8(rhs)  => lhs > rhs
          case Uint16(rhs) => lhs > rhs
          case Uint32(rhs) => lhs > rhs
          case BigInt(rhs) => lhs > BigDecimal(rhs)
          case Number(rhs) => lhs > rhs
          case _           => throw VmErrorException(WrongType)
        }
      case BigInt(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs.toInt
          case Int16(rhs)  => lhs > rhs.toInt
          case Int32(rhs)  => lhs > rhs
          case Uint8(rhs)  => lhs > rhs
          case Uint16(rhs) => lhs > rhs
          case Uint32(rhs) => lhs > rhs
          case BigInt(rhs) => lhs > rhs
          case Number(rhs) => BigDecimal(lhs) > rhs
          case _           => throw VmErrorException(WrongType)
        }
      case _ => throw VmErrorException(WrongType)
    }
    Bool(result)
  }

  private val ltImpl: (Data, Data) => Bool =
    (a, b) => gtImpl(b, a)
}
