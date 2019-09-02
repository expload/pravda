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

import com.google.protobuf.ByteString
import pravda.common.vm.Data
import pravda.common.vm.Error.WrongType
import pravda.vm.WattCounter.CpuSimpleArithmetic
import pravda.vm.operations.annotation.OpcodeImplementation
import pravda.vm.{Memory, ThrowableVmError, WattCounter}
import pravda.common.vm.Opcodes._

import scala.annotation.strictfp

/**
  * Pravda VM logical pravda.vm.Opcodes implementation.
  *
  * @see pravda.vm.Opcodes
  * @param memory Access to VM memory
  * @param wattCounter CPU, memory, storage usage counter
  */
@strictfp final class LogicalOperations(memory: Memory, wattCounter: WattCounter) {

  import Data._
  import Primitive._

  @OpcodeImplementation(
    opcode = NOT,
    description =
      "Logical NOT (negation).Pops items from the stack. If it is 'true' pushes 'false' to the stack. If it is 'false' pushes 'true' to the stack."
  )
  def not(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    val x = memory.pop()
    val r = x match {
      case Bool.True    => Bool.False
      case Bool.False   => Bool.True
      case Int8(data)   => Int8((~data).toByte)
      case Int16(data)  => Int16((~data).toShort)
      case Int32(data)  => Int32((~data).toInt)
      case Int64(data)  => Int64((~data).toLong)
      case BigInt(data) => BigInt(~data)
      case _            => throw ThrowableVmError(WrongType)
    }
    wattCounter.memoryUsage(r.volume.toLong)
    memory.push(r)
  }

  @OpcodeImplementation(
    opcode = AND,
    description = "Makes 'and' operation on two items from the stack. Pushes the result to the stack."
  )
  def and(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    binaryOperation(memory, wattCounter)(andImpl)
  }

  @OpcodeImplementation(
    opcode = OR,
    description = "Makes 'or' operation on two items from the stack. Pushes the result to the stack."
  )
  def or(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    binaryOperation(memory, wattCounter)(orImpl)
  }

  @OpcodeImplementation(
    opcode = XOR,
    description = "Makes 'xor' operation on two items from the stack. Pushes the result to the stack."
  )
  def xor(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    binaryOperation(memory, wattCounter)(xorImpl)
  }

  @OpcodeImplementation(
    opcode = EQ,
    description =
      "Checks that the top stack item is equal to the subsequent stack item. Pushes the Bool result to the stack."
  )
  def eq(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    binaryOperation(memory, wattCounter)(eqImpl)
  }

  @OpcodeImplementation(
    opcode = GT,
    description =
      "Checks that the top stack item is greater than the subsequent stack item. Pushes the Bool result to the stack."
  )
  def gt(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    binaryOperation(memory, wattCounter)(gtImpl)
  }

  @OpcodeImplementation(
    opcode = LT,
    description =
      "Checks that the top stack item is less than the subsequent stack item. Pushes the Bool result to the stack."
  )
  def lt(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic)
    binaryOperation(memory, wattCounter)(ltImpl)
  }

  //---------------------------------------------------------------------
  // Implementations
  //---------------------------------------------------------------------
  private val andImpl: (Data, Data) => Data.Primitive = { (a, b) =>
    a match {
      case Int64(lhs) =>
        b match {
          case Int8(rhs)   => Int64(lhs & rhs)
          case Int16(rhs)  => Int64(lhs & rhs)
          case Int32(rhs)  => Int64(lhs & rhs)
          case Int64(rhs)  => Int64(lhs & rhs)
          case BigInt(rhs) => BigInt(lhs & rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case Int32(lhs) =>
        b match {
          case Int8(rhs)   => Int32(lhs & rhs)
          case Int16(rhs)  => Int32(lhs & rhs)
          case Int32(rhs)  => Int32(lhs & rhs)
          case Int64(rhs)  => Int64(lhs & rhs)
          case BigInt(rhs) => BigInt(lhs & rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case Int16(lhs) =>
        b match {
          case Int8(rhs)   => Int16((lhs & rhs).toShort)
          case Int16(rhs)  => Int16((lhs & rhs).toShort)
          case Int32(rhs)  => Int32(lhs & rhs)
          case Int64(rhs)  => Int64(lhs & rhs)
          case BigInt(rhs) => BigInt(lhs.toInt & rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case Int8(lhs) =>
        b match {
          case Int8(rhs)   => Int8((lhs & rhs).toByte)
          case Int16(rhs)  => Int16((lhs & rhs).toShort)
          case Int32(rhs)  => Int32(lhs & rhs)
          case Int64(rhs)  => Int64(lhs & rhs)
          case BigInt(rhs) => BigInt(lhs.toInt & rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case BigInt(lhs) =>
        b match {
          case Int8(rhs)   => BigInt(lhs & scala.BigInt(rhs.toInt))
          case Int16(rhs)  => BigInt(lhs & scala.BigInt(rhs.toInt))
          case Int32(rhs)  => BigInt(lhs & rhs)
          case Int64(rhs)  => BigInt(lhs & rhs)
          case BigInt(rhs) => BigInt(lhs & rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case Bytes(lhs) =>
        b match {
          case Bytes(rhs) =>
            Bytes(ByteString.copyFrom(lhs.toByteArray.zipAll(rhs.toByteArray(), 0x00.toByte, 0x00.toByte).map {
              case (x: Byte, y: Byte) => (x & y).toByte
            }))
          case _ => throw ThrowableVmError(WrongType)
        }
      case Bool(lhs) =>
        b match {
          case Bool(rhs) => Bool(lhs && rhs)
          case _         => throw ThrowableVmError(WrongType)
        }
      case _ => throw ThrowableVmError(WrongType)
    }
  }

  private val orImpl: (Data, Data) => Data.Primitive = { (a, b) =>
    a match {
      case Int64(lhs) =>
        b match {
          case Int8(rhs)   => Int64(lhs | rhs)
          case Int16(rhs)  => Int64(lhs | rhs)
          case Int32(rhs)  => Int64(lhs | rhs)
          case Int64(rhs)  => Int64(lhs | rhs)
          case BigInt(rhs) => BigInt(lhs | rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case Int32(lhs) =>
        b match {
          case Int8(rhs)   => Int32(lhs | rhs)
          case Int16(rhs)  => Int32(lhs | rhs)
          case Int32(rhs)  => Int32(lhs | rhs)
          case Int64(rhs)  => Int64(lhs | rhs)
          case BigInt(rhs) => BigInt(lhs | rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case Int16(lhs) =>
        b match {
          case Int8(rhs)   => Int16((lhs | rhs).toShort)
          case Int16(rhs)  => Int16((lhs | rhs).toShort)
          case Int32(rhs)  => Int32(lhs | rhs)
          case Int64(rhs)  => Int64(lhs | rhs)
          case BigInt(rhs) => BigInt(lhs.toInt | rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case Int8(lhs) =>
        b match {
          case Int8(rhs)   => Int8((lhs | rhs).toByte)
          case Int16(rhs)  => Int16((lhs | rhs).toShort)
          case Int32(rhs)  => Int32(lhs | rhs)
          case Int64(rhs)  => Int64(lhs | rhs)
          case BigInt(rhs) => BigInt(lhs.toInt | rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case BigInt(lhs) =>
        b match {
          case Int8(rhs)   => BigInt(lhs | scala.BigInt(rhs.toInt))
          case Int16(rhs)  => BigInt(lhs | scala.BigInt(rhs.toInt))
          case Int32(rhs)  => BigInt(lhs | rhs)
          case Int64(rhs)  => BigInt(lhs | rhs)
          case BigInt(rhs) => BigInt(lhs | rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case Bytes(lhs) =>
        b match {
          case Bytes(rhs) =>
            Bytes(ByteString.copyFrom(lhs.toByteArray.zipAll(rhs.toByteArray(), 0x00.toByte, 0x00.toByte).map {
              case (x: Byte, y: Byte) => (x | y).toByte
            }))
          case _ => throw ThrowableVmError(WrongType)
        }
      case Bool(lhs) =>
        b match {
          case Bool(rhs) => Bool(lhs || rhs)
          case _         => throw ThrowableVmError(WrongType)
        }
      case _ => throw ThrowableVmError(WrongType)
    }
  }

  private val xorImpl: (Data, Data) => Data.Primitive = { (a, b) =>
    a match {
      case Int64(lhs) =>
        b match {
          case Int8(rhs)   => Int64(lhs ^ rhs)
          case Int16(rhs)  => Int64(lhs ^ rhs)
          case Int32(rhs)  => Int64(lhs ^ rhs)
          case Int64(rhs)  => Int64(lhs ^ rhs)
          case BigInt(rhs) => BigInt(lhs ^ rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case Int32(lhs) =>
        b match {
          case Int8(rhs)   => Int32(lhs ^ rhs)
          case Int16(rhs)  => Int32(lhs ^ rhs)
          case Int32(rhs)  => Int32(lhs ^ rhs)
          case Int64(rhs)  => Int64(lhs ^ rhs)
          case BigInt(rhs) => BigInt(lhs ^ rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case Int16(lhs) =>
        b match {
          case Int8(rhs)   => Int16((lhs ^ rhs).toShort)
          case Int16(rhs)  => Int16((lhs ^ rhs).toShort)
          case Int32(rhs)  => Int32(lhs ^ rhs)
          case Int64(rhs)  => Int64(lhs ^ rhs)
          case BigInt(rhs) => BigInt(lhs.toInt ^ rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case Int8(lhs) =>
        b match {
          case Int8(rhs)   => Int8((lhs ^ rhs).toByte)
          case Int16(rhs)  => Int16((lhs ^ rhs).toShort)
          case Int32(rhs)  => Int32(lhs ^ rhs)
          case Int64(rhs)  => Int64(lhs ^ rhs)
          case BigInt(rhs) => BigInt(lhs.toInt ^ rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case BigInt(lhs) =>
        b match {
          case Int8(rhs)   => BigInt(lhs ^ scala.BigInt(rhs.toInt))
          case Int16(rhs)  => BigInt(lhs ^ scala.BigInt(rhs.toInt))
          case Int32(rhs)  => BigInt(lhs ^ rhs)
          case Int64(rhs)  => BigInt(lhs ^ rhs)
          case BigInt(rhs) => BigInt(lhs ^ rhs)
          case _           => throw ThrowableVmError(WrongType)
        }
      case Bytes(lhs) =>
        b match {
          case Bytes(rhs) =>
            Bytes(ByteString.copyFrom(lhs.toByteArray.zipAll(rhs.toByteArray(), 0x00.toByte, 0x00.toByte).map {
              case (x: Byte, y: Byte) => (x ^ y).toByte
            }))
          case _ => throw ThrowableVmError(WrongType)
        }
      case Bool(lhs) =>
        b match {
          case Bool(rhs) => Bool(lhs ^ rhs)
          case _         => throw ThrowableVmError(WrongType)
        }
      case _ => throw ThrowableVmError(WrongType)
    }
  }

  private val eqImpl: (Data, Data) => Bool = { (a, b) =>
    val result = a match {
      case Int64(lhs) =>
        b match {
          case Int8(rhs)   => lhs == rhs
          case Int16(rhs)  => lhs == rhs
          case Int32(rhs)  => lhs == rhs
          case Int64(rhs)  => lhs == rhs
          case BigInt(rhs) => lhs == rhs
          case Number(rhs) => lhs == rhs
          case _           => false
        }
      case Int32(lhs) =>
        b match {
          case Int8(rhs)   => lhs == rhs
          case Int16(rhs)  => lhs == rhs
          case Int32(rhs)  => lhs == rhs
          case Int64(rhs)  => lhs == rhs
          case BigInt(rhs) => lhs == rhs
          case Number(rhs) => lhs == rhs
          case _           => false
        }
      case Int16(lhs) =>
        b match {
          case Int8(rhs)   => lhs == rhs
          case Int16(rhs)  => lhs == rhs
          case Int32(rhs)  => lhs == rhs
          case Int64(rhs)  => lhs == rhs
          case BigInt(rhs) => lhs == rhs
          case Number(rhs) => lhs == rhs
          case _           => false
        }
      case Int8(lhs) =>
        b match {
          case Int8(rhs)   => lhs == rhs
          case Int16(rhs)  => lhs == rhs
          case Int32(rhs)  => lhs == rhs
          case Int64(rhs)  => lhs == rhs
          case BigInt(rhs) => lhs == rhs
          case Number(rhs) => lhs == rhs
          case _           => false
        }
      case Number(lhs) =>
        b match {
          case Int8(rhs)   => lhs == rhs
          case Int16(rhs)  => lhs == rhs
          case Int32(rhs)  => lhs == rhs
          case Int64(rhs)  => lhs == rhs
          case BigInt(rhs) => lhs == BigDecimal(rhs)
          case Number(rhs) => lhs == rhs
          case _           => false
        }
      case BigInt(lhs) =>
        b match {
          case Int8(rhs)   => lhs == rhs
          case Int16(rhs)  => lhs == rhs
          case Int32(rhs)  => lhs == rhs
          case Int64(rhs)  => lhs == rhs
          case BigInt(rhs) => lhs == rhs
          case Number(rhs) => BigDecimal(lhs) == rhs
          case _           => false
        }
      case _ => a == b
    }
    Bool(result)
  }

  private val gtImpl: (Data, Data) => Bool = { (a, b) =>
    val result = a match {
      case Int64(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs
          case Int16(rhs)  => lhs > rhs
          case Int32(rhs)  => lhs > rhs
          case Int64(rhs)  => lhs > rhs
          case BigInt(rhs) => rhs < lhs
          case Number(rhs) => lhs > rhs
          case _           => throw ThrowableVmError(WrongType)
        }
      case Int32(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs
          case Int16(rhs)  => lhs > rhs
          case Int32(rhs)  => lhs > rhs
          case Int64(rhs)  => lhs > rhs
          case BigInt(rhs) => rhs < lhs
          case Number(rhs) => lhs > rhs
          case _           => throw ThrowableVmError(WrongType)
        }
      case Int16(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs
          case Int16(rhs)  => lhs > rhs
          case Int32(rhs)  => lhs > rhs
          case Int64(rhs)  => lhs > rhs
          case BigInt(rhs) => rhs < lhs.toInt
          case Number(rhs) => lhs > rhs
          case _           => throw ThrowableVmError(WrongType)
        }
      case Int8(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs
          case Int16(rhs)  => lhs > rhs
          case Int32(rhs)  => lhs > rhs
          case Int64(rhs)  => lhs > rhs
          case BigInt(rhs) => rhs < lhs.toInt
          case Number(rhs) => lhs > rhs
          case _           => throw ThrowableVmError(WrongType)
        }
      case Number(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs
          case Int16(rhs)  => lhs > rhs
          case Int32(rhs)  => lhs > rhs
          case Int64(rhs)  => lhs > rhs
          case BigInt(rhs) => lhs > BigDecimal(rhs)
          case Number(rhs) => lhs > rhs
          case _           => throw ThrowableVmError(WrongType)
        }
      case BigInt(lhs) =>
        b match {
          case Int8(rhs)   => lhs > rhs.toInt
          case Int16(rhs)  => lhs > rhs.toInt
          case Int32(rhs)  => lhs > rhs
          case Int64(rhs)  => lhs > rhs
          case BigInt(rhs) => lhs > rhs
          case Number(rhs) => BigDecimal(lhs) > rhs
          case _           => throw ThrowableVmError(WrongType)
        }
      case _ => throw ThrowableVmError(WrongType)
    }
    Bool(result)
  }

  private val ltImpl: (Data, Data) => Bool =
    (a, b) => gtImpl(b, a)
}
