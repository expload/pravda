package pravda.vm.operations

import pravda.vm.Data.Primitive.{BigInt, Int16, Int32, Int8, Number, Uint16, Uint32, Uint8}
import pravda.vm.VmError.WrongType
import pravda.vm.WattCounter.CpuArithmetic
import pravda.vm.{Memory, VmErrorException, WattCounter}

/**
  * Pravda VM arithmetic opcodes implementation.
  * @param memory Access to VM memory
  * @param wattCounter CPU, memory, storage usage counter
  */
final class ArithmeticOperations(memory: Memory, wattCounter: WattCounter) {

  /**
    * Makes '%' operation on two top items from stack.
    * Pushes result to stack.
    * @see pravda.vm.Opcodes.MOD
    */
  def mod(): Unit = {
    wattCounter.cpuUsage(CpuArithmetic)
    binaryOperation(memory, wattCounter) { (a, b) =>
      a match {
        case Int32(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs % rhs)
            case Int16(rhs)  => Int32(lhs % rhs)
            case Int32(rhs)  => Int32(lhs % rhs)
            case Uint8(rhs)  => Int32(lhs % rhs)
            case Uint16(rhs) => Int32(lhs % rhs)
            case Uint32(rhs) => Int32((lhs % rhs).toInt)
            case BigInt(rhs) => Int32((lhs % rhs).toInt)
            case Number(rhs) => Int32((lhs % rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Int16(lhs) =>
          b match {
            case Int8(rhs)   => Int16((lhs % rhs).toShort)
            case Int16(rhs)  => Int16((lhs % rhs).toShort)
            case Int32(rhs)  => Int16((lhs % rhs).toShort)
            case Uint8(rhs)  => Int16((lhs % rhs).toShort)
            case Uint16(rhs) => Int16((lhs % rhs).toShort)
            case Uint32(rhs) => Int16((lhs % rhs).toShort)
            case BigInt(rhs) => Int16((lhs.toInt % rhs).toShort)
            case Number(rhs) => Int16((lhs % rhs).toShort)
            case _           => throw VmErrorException(WrongType)
          }
        case Int8(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs % rhs)
            case Int16(rhs)  => Int32(lhs % rhs)
            case Int32(rhs)  => Int32(lhs % rhs)
            case Uint8(rhs)  => Int32(lhs % rhs)
            case Uint16(rhs) => Int32(lhs % rhs)
            case Uint32(rhs) => BigInt(lhs % rhs)
            case BigInt(rhs) => BigInt(lhs.toInt % rhs)
            case Number(rhs) => Number(lhs % rhs)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint8(lhs) =>
          b match {
            case Int8(rhs)   => Uint8(lhs % rhs)
            case Int16(rhs)  => Uint8(lhs % rhs)
            case Int32(rhs)  => Uint8(lhs % rhs)
            case Uint8(rhs)  => Uint8(lhs % rhs)
            case Uint16(rhs) => Uint8(lhs % rhs)
            case Uint32(rhs) => Uint8((lhs % rhs).toInt)
            case BigInt(rhs) => Uint8((lhs % rhs).toInt)
            case Number(rhs) => Uint8((lhs % rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint16(lhs) =>
          b match {
            case Int8(rhs)   => Uint16(lhs % rhs)
            case Int16(rhs)  => Uint16(lhs % rhs)
            case Int32(rhs)  => Uint16(lhs % rhs)
            case Uint8(rhs)  => Uint16(lhs % rhs)
            case Uint16(rhs) => Uint16(lhs % rhs)
            case Uint32(rhs) => Uint16((lhs % rhs).toInt)
            case BigInt(rhs) => Uint16((lhs % rhs).toInt)
            case Number(rhs) => Uint16((lhs % rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint32(lhs) =>
          b match {
            case Int8(rhs)   => Uint32(lhs % rhs)
            case Int16(rhs)  => Uint32(lhs % rhs)
            case Int32(rhs)  => Uint32(lhs % rhs)
            case Uint8(rhs)  => Uint32(lhs % rhs)
            case Uint16(rhs) => Uint32(lhs % rhs)
            case Uint32(rhs) => Uint32(lhs % rhs)
            case BigInt(rhs) => Uint32((lhs % rhs).toLong)
            case Number(rhs) => Uint32((lhs % rhs).toLong)
            case _           => throw VmErrorException(WrongType)
          }
        case Number(lhs) =>
          b match {
            case Int8(rhs)   => Number(lhs % rhs)
            case Int16(rhs)  => Number(lhs % rhs)
            case Int32(rhs)  => Number(lhs % rhs)
            case Uint8(rhs)  => Number(lhs % rhs)
            case Uint16(rhs) => Number(lhs % rhs)
            case Uint32(rhs) => Number(lhs % rhs)
            case BigInt(rhs) => Number((lhs % BigDecimal(rhs)).toDouble)
            case Number(rhs) => Number(lhs % rhs)
            case _           => throw VmErrorException(WrongType)
          }
        case BigInt(lhs) =>
          b match {
            case Int8(rhs)   => BigInt(lhs % scala.BigInt(rhs.toInt))
            case Int16(rhs)  => BigInt(lhs % scala.BigInt(rhs.toInt))
            case Int32(rhs)  => BigInt(lhs % rhs)
            case Uint8(rhs)  => BigInt(lhs % rhs)
            case Uint16(rhs) => BigInt(lhs % rhs)
            case Uint32(rhs) => BigInt(lhs % rhs)
            case BigInt(rhs) => BigInt(lhs % rhs)
            case Number(rhs) => BigInt(lhs % rhs.toLong)
            case _           => throw VmErrorException(WrongType)
          }
        case _ => throw VmErrorException(WrongType)
      }
    }
  }

  /**
    * Makes '+' operation on two top items from stack.
    * Pushes result to stack.
    * @see pravda.vm.Opcodes$.ADD
    */
  def add(): Unit = {
    wattCounter.cpuUsage(CpuArithmetic)
    binaryOperation(memory, wattCounter) { (a, b) =>
      a match {
        case Int32(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs + rhs)
            case Int16(rhs)  => Int32(lhs + rhs)
            case Int32(rhs)  => Int32(lhs + rhs)
            case Uint8(rhs)  => Int32(lhs + rhs)
            case Uint16(rhs) => Int32(lhs + rhs)
            case Uint32(rhs) => Int32((lhs + rhs).toInt)
            case BigInt(rhs) => Int32((lhs + rhs).toInt)
            case Number(rhs) => Int32((lhs + rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Int16(lhs) =>
          b match {
            case Int8(rhs)   => Int16((lhs + rhs).toShort)
            case Int16(rhs)  => Int16((lhs + rhs).toShort)
            case Int32(rhs)  => Int16((lhs + rhs).toShort)
            case Uint8(rhs)  => Int16((lhs + rhs).toShort)
            case Uint16(rhs) => Int16((lhs + rhs).toShort)
            case Uint32(rhs) => Int16((lhs + rhs).toShort)
            case BigInt(rhs) => Int16((lhs.toInt + rhs).toShort)
            case Number(rhs) => Int16((lhs + rhs).toShort)
            case _           => throw VmErrorException(WrongType)
          }
        case Int8(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs + rhs)
            case Int16(rhs)  => Int32(lhs + rhs)
            case Int32(rhs)  => Int32(lhs + rhs)
            case Uint8(rhs)  => Int32(lhs + rhs)
            case Uint16(rhs) => Int32(lhs + rhs)
            case Uint32(rhs) => BigInt(lhs + rhs)
            case BigInt(rhs) => BigInt(lhs.toInt + rhs)
            case Number(rhs) => Number(lhs + rhs)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint8(lhs) =>
          b match {
            case Int8(rhs)   => Uint8(lhs + rhs)
            case Int16(rhs)  => Uint8(lhs + rhs)
            case Int32(rhs)  => Uint8(lhs + rhs)
            case Uint8(rhs)  => Uint8(lhs + rhs)
            case Uint16(rhs) => Uint8(lhs + rhs)
            case Uint32(rhs) => Uint8((lhs + rhs).toInt)
            case BigInt(rhs) => Uint8((lhs + rhs).toInt)
            case Number(rhs) => Uint8((lhs + rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint16(lhs) =>
          b match {
            case Int8(rhs)   => Uint16(lhs + rhs)
            case Int16(rhs)  => Uint16(lhs + rhs)
            case Int32(rhs)  => Uint16(lhs + rhs)
            case Uint8(rhs)  => Uint16(lhs + rhs)
            case Uint16(rhs) => Uint16(lhs + rhs)
            case Uint32(rhs) => Uint16((lhs + rhs).toInt)
            case BigInt(rhs) => Uint16((lhs + rhs).toInt)
            case Number(rhs) => Uint16((lhs + rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint32(lhs) =>
          b match {
            case Int8(rhs)   => Uint32(lhs + rhs)
            case Int16(rhs)  => Uint32(lhs + rhs)
            case Int32(rhs)  => Uint32(lhs + rhs)
            case Uint8(rhs)  => Uint32(lhs + rhs)
            case Uint16(rhs) => Uint32(lhs + rhs)
            case Uint32(rhs) => Uint32(lhs + rhs)
            case BigInt(rhs) => Uint32((lhs + rhs).toLong)
            case Number(rhs) => Uint32((lhs + rhs).toLong)
            case _           => throw VmErrorException(WrongType)
          }
        case Number(lhs) =>
          b match {
            case Int8(rhs)   => Number(lhs + rhs)
            case Int16(rhs)  => Number(lhs + rhs)
            case Int32(rhs)  => Number(lhs + rhs)
            case Uint8(rhs)  => Number(lhs + rhs)
            case Uint16(rhs) => Number(lhs + rhs)
            case Uint32(rhs) => Number(lhs + rhs)
            case BigInt(rhs) => Number((lhs + BigDecimal(rhs)).toDouble)
            case Number(rhs) => Number(lhs + rhs)
            case _           => throw VmErrorException(WrongType)
          }
        case BigInt(lhs) =>
          b match {
            case Int8(rhs)   => BigInt(lhs + scala.BigInt(rhs.toInt))
            case Int16(rhs)  => BigInt(lhs + scala.BigInt(rhs.toInt))
            case Int32(rhs)  => BigInt(lhs + rhs)
            case Uint8(rhs)  => BigInt(lhs + rhs)
            case Uint16(rhs) => BigInt(lhs + rhs)
            case Uint32(rhs) => BigInt(lhs + rhs)
            case BigInt(rhs) => BigInt(lhs + rhs)
            case Number(rhs) => BigInt(lhs + rhs.toLong)
            case _           => throw VmErrorException(WrongType)
          }
        case _ => throw VmErrorException(WrongType)
      }
    }
  }

  /**
    * Makes '/' operation on two top items from stack.
    * Pushes result to stack.
    * @see pravda.vm.Opcodes.DIV
    */
  def div(): Unit = {
    wattCounter.cpuUsage(CpuArithmetic)
    binaryOperation(memory, wattCounter) { (a, b) =>
      a match {
        case Int32(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs / rhs)
            case Int16(rhs)  => Int32(lhs / rhs)
            case Int32(rhs)  => Int32(lhs / rhs)
            case Uint8(rhs)  => Int32(lhs / rhs)
            case Uint16(rhs) => Int32(lhs / rhs)
            case Uint32(rhs) => Int32((lhs / rhs).toInt)
            case BigInt(rhs) => Int32((lhs / rhs).toInt)
            case Number(rhs) => Int32((lhs / rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Int16(lhs) =>
          b match {
            case Int8(rhs)   => Int16((lhs / rhs).toShort)
            case Int16(rhs)  => Int16((lhs / rhs).toShort)
            case Int32(rhs)  => Int16((lhs / rhs).toShort)
            case Uint8(rhs)  => Int16((lhs / rhs).toShort)
            case Uint16(rhs) => Int16((lhs / rhs).toShort)
            case Uint32(rhs) => Int16((lhs / rhs).toShort)
            case BigInt(rhs) => Int16((lhs.toInt / rhs).toShort)
            case Number(rhs) => Int16((lhs / rhs).toShort)
            case _           => throw VmErrorException(WrongType)
          }
        case Int8(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs / rhs)
            case Int16(rhs)  => Int32(lhs / rhs)
            case Int32(rhs)  => Int32(lhs / rhs)
            case Uint8(rhs)  => Int32(lhs / rhs)
            case Uint16(rhs) => Int32(lhs / rhs)
            case Uint32(rhs) => BigInt(lhs / rhs)
            case BigInt(rhs) => BigInt(lhs.toInt / rhs)
            case Number(rhs) => Number(lhs / rhs)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint8(lhs) =>
          b match {
            case Int8(rhs)   => Uint8(lhs / rhs)
            case Int16(rhs)  => Uint8(lhs / rhs)
            case Int32(rhs)  => Uint8(lhs / rhs)
            case Uint8(rhs)  => Uint8(lhs / rhs)
            case Uint16(rhs) => Uint8(lhs / rhs)
            case Uint32(rhs) => Uint8((lhs / rhs).toInt)
            case BigInt(rhs) => Uint8((lhs / rhs).toInt)
            case Number(rhs) => Uint8((lhs / rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint16(lhs) =>
          b match {
            case Int8(rhs)   => Uint16(lhs / rhs)
            case Int16(rhs)  => Uint16(lhs / rhs)
            case Int32(rhs)  => Uint16(lhs / rhs)
            case Uint8(rhs)  => Uint16(lhs / rhs)
            case Uint16(rhs) => Uint16(lhs / rhs)
            case Uint32(rhs) => Uint16((lhs / rhs).toInt)
            case BigInt(rhs) => Uint16((lhs / rhs).toInt)
            case Number(rhs) => Uint16((lhs / rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint32(lhs) =>
          b match {
            case Int8(rhs)   => Uint32(lhs / rhs)
            case Int16(rhs)  => Uint32(lhs / rhs)
            case Int32(rhs)  => Uint32(lhs / rhs)
            case Uint8(rhs)  => Uint32(lhs / rhs)
            case Uint16(rhs) => Uint32(lhs / rhs)
            case Uint32(rhs) => Uint32(lhs / rhs)
            case BigInt(rhs) => Uint32((lhs / rhs).toLong)
            case Number(rhs) => Uint32((lhs / rhs).toLong)
            case _           => throw VmErrorException(WrongType)
          }
        case Number(lhs) =>
          b match {
            case Int8(rhs)   => Number(lhs / rhs)
            case Int16(rhs)  => Number(lhs / rhs)
            case Int32(rhs)  => Number(lhs / rhs)
            case Uint8(rhs)  => Number(lhs / rhs)
            case Uint16(rhs) => Number(lhs / rhs)
            case Uint32(rhs) => Number(lhs / rhs)
            case BigInt(rhs) => Number((lhs / BigDecimal(rhs)).toDouble)
            case Number(rhs) => Number(lhs / rhs)
            case _           => throw VmErrorException(WrongType)
          }
        case BigInt(lhs) =>
          b match {
            case Int8(rhs)   => BigInt(lhs / scala.BigInt(rhs.toInt))
            case Int16(rhs)  => BigInt(lhs / scala.BigInt(rhs.toInt))
            case Int32(rhs)  => BigInt(lhs / rhs)
            case Uint8(rhs)  => BigInt(lhs / rhs)
            case Uint16(rhs) => BigInt(lhs / rhs)
            case Uint32(rhs) => BigInt(lhs / rhs)
            case BigInt(rhs) => BigInt(lhs / rhs)
            case Number(rhs) => BigInt(lhs / rhs.toLong)
            case _           => throw VmErrorException(WrongType)
          }
        case _ => throw VmErrorException(WrongType)
      }
    }
  }

  /**
    * Makes '*' operation on two top items from stack.
    * Pushes result to stack.
    * @see pravda.vm.Opcodes.MUL
    */
  def mul(): Unit = {
    wattCounter.cpuUsage(CpuArithmetic)
    binaryOperation(memory, wattCounter) { (a, b) =>
      a match {
        case Int32(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs * rhs)
            case Int16(rhs)  => Int32(lhs * rhs)
            case Int32(rhs)  => Int32(lhs * rhs)
            case Uint8(rhs)  => Int32(lhs * rhs)
            case Uint16(rhs) => Int32(lhs * rhs)
            case Uint32(rhs) => Int32((lhs * rhs).toInt)
            case BigInt(rhs) => Int32((lhs * rhs).toInt)
            case Number(rhs) => Int32((lhs * rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Int16(lhs) =>
          b match {
            case Int8(rhs)   => Int16((lhs * rhs).toShort)
            case Int16(rhs)  => Int16((lhs * rhs).toShort)
            case Int32(rhs)  => Int16((lhs * rhs).toShort)
            case Uint8(rhs)  => Int16((lhs * rhs).toShort)
            case Uint16(rhs) => Int16((lhs * rhs).toShort)
            case Uint32(rhs) => Int16((lhs * rhs).toShort)
            case BigInt(rhs) => Int16((lhs.toInt * rhs).toShort)
            case Number(rhs) => Int16((lhs * rhs).toShort)
            case _           => throw VmErrorException(WrongType)
          }
        case Int8(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs * rhs)
            case Int16(rhs)  => Int32(lhs * rhs)
            case Int32(rhs)  => Int32(lhs * rhs)
            case Uint8(rhs)  => Int32(lhs * rhs)
            case Uint16(rhs) => Int32(lhs * rhs)
            case Uint32(rhs) => BigInt(lhs * rhs)
            case BigInt(rhs) => BigInt(lhs.toInt * rhs)
            case Number(rhs) => Number(lhs * rhs)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint8(lhs) =>
          b match {
            case Int8(rhs)   => Uint8(lhs * rhs)
            case Int16(rhs)  => Uint8(lhs * rhs)
            case Int32(rhs)  => Uint8(lhs * rhs)
            case Uint8(rhs)  => Uint8(lhs * rhs)
            case Uint16(rhs) => Uint8(lhs * rhs)
            case Uint32(rhs) => Uint8((lhs * rhs).toInt)
            case BigInt(rhs) => Uint8((lhs * rhs).toInt)
            case Number(rhs) => Uint8((lhs * rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint16(lhs) =>
          b match {
            case Int8(rhs)   => Uint16(lhs * rhs)
            case Int16(rhs)  => Uint16(lhs * rhs)
            case Int32(rhs)  => Uint16(lhs * rhs)
            case Uint8(rhs)  => Uint16(lhs * rhs)
            case Uint16(rhs) => Uint16(lhs * rhs)
            case Uint32(rhs) => Uint16((lhs * rhs).toInt)
            case BigInt(rhs) => Uint16((lhs * rhs).toInt)
            case Number(rhs) => Uint16((lhs * rhs).toInt)
            case _           => throw VmErrorException(WrongType)
          }
        case Uint32(lhs) =>
          b match {
            case Int8(rhs)   => Uint32(lhs * rhs)
            case Int16(rhs)  => Uint32(lhs * rhs)
            case Int32(rhs)  => Uint32(lhs * rhs)
            case Uint8(rhs)  => Uint32(lhs * rhs)
            case Uint16(rhs) => Uint32(lhs * rhs)
            case Uint32(rhs) => Uint32(lhs * rhs)
            case BigInt(rhs) => Uint32((lhs * rhs).toLong)
            case Number(rhs) => Uint32((lhs * rhs).toLong)
            case _           => throw VmErrorException(WrongType)
          }
        case Number(lhs) =>
          b match {
            case Int8(rhs)   => Number(lhs * rhs)
            case Int16(rhs)  => Number(lhs * rhs)
            case Int32(rhs)  => Number(lhs * rhs)
            case Uint8(rhs)  => Number(lhs * rhs)
            case Uint16(rhs) => Number(lhs * rhs)
            case Uint32(rhs) => Number(lhs * rhs)
            case BigInt(rhs) => Number((lhs * BigDecimal(rhs)).toDouble)
            case Number(rhs) => Number(lhs * rhs)
            case _           => throw VmErrorException(WrongType)
          }
        case BigInt(lhs) =>
          b match {
            case Int8(rhs)   => BigInt(lhs * scala.BigInt(rhs.toInt))
            case Int16(rhs)  => BigInt(lhs * scala.BigInt(rhs.toInt))
            case Int32(rhs)  => BigInt(lhs * rhs)
            case Uint8(rhs)  => BigInt(lhs * rhs)
            case Uint16(rhs) => BigInt(lhs * rhs)
            case Uint32(rhs) => BigInt(lhs * rhs)
            case BigInt(rhs) => BigInt(lhs * rhs)
            case Number(rhs) => BigInt(lhs * rhs.toLong)
            case _           => throw VmErrorException(WrongType)
          }
        case _ => throw VmErrorException(WrongType)
      }
    }
  }
}
