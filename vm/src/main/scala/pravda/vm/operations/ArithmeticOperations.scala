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

import pravda.vm.Data.Primitive.{BigInt, Int16, Int32, Int64, Int8, Number}
import pravda.vm.Error.WrongType
import pravda.vm.WattCounter.CpuArithmetic
import pravda.vm.operations.annotation.OpcodeImplementation
import pravda.vm.{Memory, Opcodes, ThrowableVmError, WattCounter}

/**
  * Pravda VM arithmetic opcodes implementation.
  *
  * @param memory Access to VM memory
  * @param wattCounter CPU, memory, storage usage counter
  */
final class ArithmeticOperations(memory: Memory, wattCounter: WattCounter) {

  @OpcodeImplementation(
    opcode = Opcodes.MOD,
    description = "Makes '%' operation on two top items from stack. Pushes result to stack."
  )
  def mod(): Unit = {
    wattCounter.cpuUsage(CpuArithmetic)
    binaryOperation(memory, wattCounter) { (a, b) =>
      a match {
        case Int64(lhs) =>
          b match {
            case Int8(rhs)   => Int64(lhs % rhs)
            case Int16(rhs)  => Int64(lhs % rhs)
            case Int32(rhs)  => Int64(lhs % rhs)
            case Int64(rhs)  => Int64(lhs % rhs)
            case BigInt(rhs) => BigInt(lhs % rhs)
            case Number(rhs) => Number(lhs % rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Int32(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs % rhs)
            case Int16(rhs)  => Int32(lhs % rhs)
            case Int32(rhs)  => Int32(lhs % rhs)
            case Int64(rhs)  => Int64(lhs % rhs)
            case BigInt(rhs) => BigInt(lhs % rhs)
            case Number(rhs) => Number(lhs % rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Int16(lhs) =>
          b match {
            case Int8(rhs)   => Int16((lhs % rhs).toShort)
            case Int16(rhs)  => Int16((lhs % rhs).toShort)
            case Int32(rhs)  => Int32(lhs % rhs)
            case Int64(rhs)  => Int64(lhs % rhs)
            case BigInt(rhs) => BigInt(lhs.toInt % rhs)
            case Number(rhs) => Number(lhs % rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Int8(lhs) =>
          b match {
            case Int8(rhs)   => Int8((lhs % rhs).toByte)
            case Int16(rhs)  => Int16((lhs % rhs).toShort)
            case Int32(rhs)  => Int32(lhs % rhs)
            case Int64(rhs)  => Int64(lhs % rhs)
            case BigInt(rhs) => BigInt(lhs.toInt % rhs)
            case Number(rhs) => Number(lhs % rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Number(lhs) =>
          b match {
            case Int8(rhs)   => Number(lhs % rhs)
            case Int16(rhs)  => Number(lhs % rhs)
            case Int32(rhs)  => Number(lhs % rhs)
            case Int64(rhs)  => Number(lhs % rhs)
            case BigInt(rhs) => Number((lhs % BigDecimal(rhs)).toDouble)
            case Number(rhs) => Number(lhs % rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case BigInt(lhs) =>
          b match {
            case Int8(rhs)   => BigInt(lhs % scala.BigInt(rhs.toInt))
            case Int16(rhs)  => BigInt(lhs % scala.BigInt(rhs.toInt))
            case Int32(rhs)  => BigInt(lhs % rhs)
            case Int64(rhs)  => BigInt(lhs % rhs)
            case BigInt(rhs) => BigInt(lhs % rhs)
            case Number(rhs) => Number(lhs.toDouble % rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case _ => throw ThrowableVmError(WrongType)
      }
    }
  }

  @OpcodeImplementation(
    opcode = Opcodes.ADD,
    description = "Makes '+' operation on two top items from stack. Pushes result to stack."
  )
  def add(): Unit = {
    wattCounter.cpuUsage(CpuArithmetic)
    binaryOperation(memory, wattCounter) { (a, b) =>
      a match {
        case Int64(lhs) =>
          b match {
            case Int8(rhs)   => Int64(lhs + rhs)
            case Int16(rhs)  => Int64(lhs + rhs)
            case Int32(rhs)  => Int64(lhs + rhs)
            case Int64(rhs)  => Int64(lhs + rhs)
            case BigInt(rhs) => BigInt(lhs + rhs)
            case Number(rhs) => Number(lhs + rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Int32(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs + rhs)
            case Int16(rhs)  => Int32(lhs + rhs)
            case Int32(rhs)  => Int32(lhs + rhs)
            case Int64(rhs)  => Int64(lhs + rhs)
            case BigInt(rhs) => BigInt(lhs + rhs)
            case Number(rhs) => Number(lhs + rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Int16(lhs) =>
          b match {
            case Int8(rhs)   => Int16((lhs + rhs).toShort)
            case Int16(rhs)  => Int16((lhs + rhs).toShort)
            case Int32(rhs)  => Int32(lhs + rhs)
            case Int64(rhs)  => Int64(lhs + rhs)
            case BigInt(rhs) => BigInt(lhs.toInt + rhs)
            case Number(rhs) => Number(lhs + rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Int8(lhs) =>
          b match {
            case Int8(rhs)   => Int8((lhs + rhs).toByte)
            case Int16(rhs)  => Int16((lhs + rhs).toShort)
            case Int32(rhs)  => Int32(lhs + rhs)
            case Int64(rhs)  => Int64(lhs + rhs)
            case BigInt(rhs) => BigInt(lhs.toInt + rhs)
            case Number(rhs) => Number(lhs + rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Number(lhs) =>
          b match {
            case Int8(rhs)   => Number(lhs + rhs)
            case Int16(rhs)  => Number(lhs + rhs)
            case Int32(rhs)  => Number(lhs + rhs)
            case Int64(rhs)  => Number(lhs + rhs)
            case BigInt(rhs) => Number((lhs + BigDecimal(rhs)).toDouble)
            case Number(rhs) => Number(lhs + rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case BigInt(lhs) =>
          b match {
            case Int8(rhs)   => BigInt(lhs + scala.BigInt(rhs.toInt))
            case Int16(rhs)  => BigInt(lhs + scala.BigInt(rhs.toInt))
            case Int32(rhs)  => BigInt(lhs + rhs)
            case Int64(rhs)  => BigInt(lhs + rhs)
            case BigInt(rhs) => BigInt(lhs + rhs)
            case Number(rhs) => Number(lhs.toDouble + rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case _ => throw ThrowableVmError(WrongType)
      }
    }
  }

  @OpcodeImplementation(
    opcode = Opcodes.DIV,
    description = "Makes '/' operation on two top items from stack. Pushes result to stack."
  )
  def div(): Unit = {
    wattCounter.cpuUsage(CpuArithmetic)
    binaryOperation(memory, wattCounter) { (a, b) =>
      a match {
        case Int64(lhs) =>
          b match {
            case Int8(rhs)   => Int64(lhs / rhs)
            case Int16(rhs)  => Int64(lhs / rhs)
            case Int32(rhs)  => Int64(lhs / rhs)
            case Int64(rhs)  => Int64(lhs / rhs)
            case BigInt(rhs) => BigInt(lhs / rhs)
            case Number(rhs) => Number(lhs / rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Int32(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs / rhs)
            case Int16(rhs)  => Int32(lhs / rhs)
            case Int32(rhs)  => Int32(lhs / rhs)
            case Int64(rhs)  => Int64(lhs / rhs)
            case BigInt(rhs) => BigInt(lhs / rhs)
            case Number(rhs) => Number(lhs / rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Int16(lhs) =>
          b match {
            case Int8(rhs)   => Int16((lhs / rhs).toShort)
            case Int16(rhs)  => Int16((lhs / rhs).toShort)
            case Int32(rhs)  => Int32(lhs / rhs)
            case Int64(rhs)  => Int64(lhs / rhs)
            case BigInt(rhs) => BigInt(lhs.toInt / rhs)
            case Number(rhs) => Number(lhs / rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Int8(lhs) =>
          b match {
            case Int8(rhs)   => Int8((lhs / rhs).toByte)
            case Int16(rhs)  => Int16((lhs / rhs).toShort)
            case Int32(rhs)  => Int32(lhs / rhs)
            case Int64(rhs)  => Int64(lhs / rhs)
            case BigInt(rhs) => BigInt(lhs.toInt / rhs)
            case Number(rhs) => Number(lhs / rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Number(lhs) =>
          b match {
            case Int8(rhs)   => Number(lhs / rhs)
            case Int16(rhs)  => Number(lhs / rhs)
            case Int32(rhs)  => Number(lhs / rhs)
            case Int64(rhs)  => Number(lhs / rhs)
            case BigInt(rhs) => Number((lhs / BigDecimal(rhs)).toDouble)
            case Number(rhs) => Number(lhs / rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case BigInt(lhs) =>
          b match {
            case Int8(rhs)   => BigInt(lhs / scala.BigInt(rhs.toInt))
            case Int16(rhs)  => BigInt(lhs / scala.BigInt(rhs.toInt))
            case Int32(rhs)  => BigInt(lhs / rhs)
            case Int64(rhs)  => BigInt(lhs / rhs)
            case BigInt(rhs) => BigInt(lhs / rhs)
            case Number(rhs) => Number(lhs.toDouble / rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case _ => throw ThrowableVmError(WrongType)
      }
    }
  }

  @OpcodeImplementation(
    opcode = Opcodes.MUL,
    description = "Makes '*' operation on two top items from stack. Pushes result to stack."
  )
  def mul(): Unit = {
    wattCounter.cpuUsage(CpuArithmetic)
    binaryOperation(memory, wattCounter) { (a, b) =>
      a match {
        case Int32(lhs) =>
          b match {
            case Int8(rhs)   => Int32(lhs * rhs)
            case Int16(rhs)  => Int32(lhs * rhs)
            case Int32(rhs)  => Int32(lhs * rhs)
            case Int64(rhs)  => Int64(lhs * rhs)
            case BigInt(rhs) => BigInt(lhs * rhs)
            case Number(rhs) => Number(lhs * rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Int16(lhs) =>
          b match {
            case Int8(rhs)   => Int16((lhs * rhs).toShort)
            case Int16(rhs)  => Int16((lhs * rhs).toShort)
            case Int32(rhs)  => Int32(lhs * rhs)
            case Int64(rhs)  => Int64(lhs * rhs)
            case BigInt(rhs) => BigInt(lhs.toInt * rhs)
            case Number(rhs) => Number(lhs * rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Int8(lhs) =>
          b match {
            case Int8(rhs)   => Int8((lhs * rhs).toByte)
            case Int16(rhs)  => Int16((lhs * rhs).toShort)
            case Int32(rhs)  => Int32(lhs * rhs)
            case Int64(rhs)  => Int64(lhs * rhs)
            case BigInt(rhs) => BigInt(lhs.toInt * rhs)
            case Number(rhs) => Number(lhs * rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case Number(lhs) =>
          b match {
            case Int8(rhs)   => Number(lhs * rhs)
            case Int16(rhs)  => Number(lhs * rhs)
            case Int32(rhs)  => Number(lhs * rhs)
            case Int64(rhs)  => Number(lhs * rhs)
            case BigInt(rhs) => Number((lhs * BigDecimal(rhs)).toDouble)
            case Number(rhs) => Number(lhs * rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case BigInt(lhs) =>
          b match {
            case Int8(rhs)   => BigInt(lhs * scala.BigInt(rhs.toInt))
            case Int16(rhs)  => BigInt(lhs * scala.BigInt(rhs.toInt))
            case Int32(rhs)  => BigInt(lhs * rhs)
            case Int64(rhs)  => BigInt(lhs * rhs)
            case BigInt(rhs) => BigInt(lhs * rhs)
            case Number(rhs) => Number(lhs.toDouble * rhs)
            case _           => throw ThrowableVmError(WrongType)
          }
        case _ => throw ThrowableVmError(WrongType)
      }
    }
  }
}
