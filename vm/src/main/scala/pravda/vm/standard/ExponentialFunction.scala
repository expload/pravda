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

package pravda.vm.standard

import pravda.vm.Data.Primitive._
import pravda.vm.Data.Type
import pravda.vm.Error.WrongType
import pravda.vm.WattCounter.CpuArithmetic
import pravda.vm._

object ExponentialFunction extends FunctionDefinition {
  val id = 0x03L

  val description =
    "Takes two items from the stack, raises the second number to a power of first number and pushes the result to the stack."

  val args: Seq[(String, Seq[Type])] = Seq(
    "x" -> Seq(Data.Type.Int8, Data.Type.Int16, Data.Type.Int32, Data.Type.Int64, Data.Type.Number, Data.Type.BigInt),
    "y" -> Seq(Data.Type.Int8, Data.Type.Int16, Data.Type.Int32, Data.Type.Number, Data.Type.BigInt)
  )

  val returns = Seq(Data.Type.Int8, Data.Type.Int16, Data.Type.Int32, Data.Type.Int64, Data.Type.Number, Data.Type.BigInt)

  private def pow(x: Long, y: Int): Long = y match {
    case p if p <= 0     => 1L
    case p if p % 2 == 1 => x * pow(x, y - 1)
    case p if p % 2 == 0 =>
      val r = pow(x, p / 2)
      r * r
  }
  private def pow(x: Int, y: Int): Int = y match {
    case p if p <= 0     => 1
    case p if p % 2 == 1 => x * pow(x, y - 1)
    case p if p % 2 == 0 =>
      val r = pow(x, p / 2)
      r * r
  }
  private def pow(x: Short, y: Int): Short = y match {
    case p if p <= 0     => 1
    case p if p % 2 == 1 => (x * pow(x, y - 1)).toShort
    case p if p % 2 == 0 =>
      val r = pow(x, p / 2)
      (r * r).toShort
  }
  private def pow(x: Byte, y: Int): Byte = y match {
    case p if p <= 0     => 1
    case p if p % 2 == 1 => (x * pow(x, y - 1)).toByte
    case p if p % 2 == 0 =>
      val r = pow(x, p / 2)
      (r * r).toByte
  }

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {

    def calculateWatts(primitive: Data.Primitive): Data.Primitive = {
      wattCounter.memoryUsage(primitive.volume.toLong)
      primitive
    }

    def wattsForBigInt(x: scala.BigInt, y: Int): Long =
      x.bitLength * y.abs.toLong / 8 + 1

    wattCounter.cpuUsage(CpuArithmetic)
    val a = memory.pop()
    val b = memory.pop()

    val res = a match {
      case Int64(lhs) =>
        b match {
          case Int8(rhs) =>
            calculateWatts(Int64(pow(lhs, rhs.toInt)))
          case Int16(rhs) =>
            calculateWatts(Int64(pow(lhs, rhs.toInt)))
          case Int32(rhs) =>
            calculateWatts(Int64(pow(lhs, rhs)))
          case Number(rhs) =>
            calculateWatts(Number(math.pow(lhs.toDouble, rhs)))
          case _ => throw ThrowableVmError(WrongType)
        }
      case Int32(lhs) =>
        b match {
          case Int8(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs.toInt)))
          case Int16(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs.toInt)))
          case Int32(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case Number(rhs) =>
            calculateWatts(Number(math.pow(lhs.toDouble, rhs)))
          case _ => throw ThrowableVmError(WrongType)
        }
      case Int16(lhs) =>
        b match {
          case Int8(rhs) =>
            calculateWatts(Int16(pow(lhs, rhs.toInt)))
          case Int16(rhs) =>
            calculateWatts(Int16(pow(lhs, rhs.toInt)))
          case Int32(rhs) =>
            calculateWatts(Int16(pow(lhs, rhs)))
          case Number(rhs) =>
            calculateWatts(Number(math.pow(lhs.toDouble, rhs)))
          case _ => throw ThrowableVmError(WrongType)
        }
      case Int8(lhs) =>
        b match {
          case Int8(rhs) =>
            calculateWatts(Int8(pow(lhs, rhs.toInt)))
          case Int16(rhs) =>
            calculateWatts(Int8(pow(lhs, rhs.toInt)))
          case Int32(rhs) =>
            calculateWatts(Int8(pow(lhs, rhs)))
          case Number(rhs) =>
            calculateWatts(Number(math.pow(lhs.toDouble, rhs)))
          case _ => throw ThrowableVmError(WrongType)
        }
      case Number(lhs) =>
        b match {
          case Int8(rhs) =>
            calculateWatts(Number(math.pow(lhs, rhs.toDouble)))
          case Int16(rhs) =>
            calculateWatts(Number(math.pow(lhs, rhs.toDouble)))
          case Int32(rhs) =>
            calculateWatts(Number(math.pow(lhs, rhs.toDouble)))
          case Number(rhs) =>
            calculateWatts(Number(math.pow(lhs, rhs)))
          case _ => throw ThrowableVmError(WrongType)
        }
      case BigInt(lhs) =>
        b match {
          case Int8(rhs) =>
            wattCounter.memoryUsage(wattsForBigInt(lhs, rhs.toInt))
            BigInt(lhs.pow(rhs.toInt))
          case Int16(rhs) =>
            wattCounter.memoryUsage(wattsForBigInt(lhs, rhs.toInt))
            BigInt(lhs.pow(rhs.toInt))
          case Int32(rhs) =>
            wattCounter.memoryUsage(wattsForBigInt(lhs, rhs))
            BigInt(lhs.pow(rhs))
          case BigInt(rhs) =>
            wattCounter.memoryUsage(wattsForBigInt(lhs, rhs.toInt))
            BigInt(lhs.pow(rhs.toInt))
          case _ => throw ThrowableVmError(WrongType)
        }
      case _ => throw ThrowableVmError(WrongType)
    }

    memory.push(res)
  }
}
