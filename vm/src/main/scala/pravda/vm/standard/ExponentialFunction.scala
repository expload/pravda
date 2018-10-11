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
import pravda.vm.VmError.WrongType
import pravda.vm.WattCounter.CpuArithmetic
import pravda.vm._

object ExponentialFunction extends FunctionDefinition {
  val id = 0x03L

  val description =
    "Takes two items from the stack, raises the second number to a power of first number and pushes the result to the stack."

  val args: Seq[(String, Seq[Type])] = Seq(
    "x" -> Seq(Data.Type.Int32),
    "y" -> Seq(Data.Type.Int32)
  )

  val returns = Seq(Data.Type.Number)

  private def pow(x: Double, y: Double): Double = math.pow(x, y)
  private def pow(x: Double, y: Byte): Double = math.pow(x, y.toDouble)
  private def pow(x: Double, y: Int): Double = math.pow(x, y.toDouble)
  private def pow(x: Double, y: Long): Double = math.pow(x, y.toDouble)
  private def pow(x: Double, y: Short): Double = math.pow(x, y.toDouble)
  private def pow(x: Long, y: Double): Long = math.pow(x.toDouble, y).toLong
  private def pow(x: Long, y: Byte): Long = math.pow(x.toDouble, y.toDouble).toLong
  private def pow(x: Long, y: Int): Long = math.pow(x.toDouble, y.toDouble).toLong
  private def pow(x: Long, y: Long): Long = math.pow(x.toDouble, y.toDouble).toLong
  private def pow(x: Long, y: Short): Long = math.pow(x.toDouble, y.toDouble).toLong
  private def pow(x: Int, y: Double): Int = math.pow(x.toDouble, y).toInt
  private def pow(x: Int, y: Byte): Int = math.pow(x.toDouble, y.toDouble).toInt
  private def pow(x: Int, y: Int): Int = math.pow(x.toDouble, y.toDouble).toInt
  private def pow(x: Int, y: Long): Int = math.pow(x.toDouble, y.toDouble).toInt
  private def pow(x: Int, y: Short): Int = math.pow(x.toDouble, y.toDouble).toInt
  private def pow(x: Short, y: Double): Short = math.pow(x.toDouble, y).toShort
  private def pow(x: Short, y: Byte): Short = math.pow(x.toDouble, y.toDouble).toShort
  private def pow(x: Short, y: Int): Short = math.pow(x.toDouble, y.toDouble).toShort
  private def pow(x: Short, y: Long): Short = math.pow(x.toDouble, y.toDouble).toShort
  private def pow(x: Short, y: Short): Short = math.pow(x.toDouble, y.toDouble).toShort
  private def pow(x: Byte, y: Double): Int = math.pow(x.toDouble, y).toInt
  private def pow(x: Byte, y: Byte): Int = math.pow(x.toDouble, y.toDouble).toInt
  private def pow(x: Byte, y: Int): Int = math.pow(x.toDouble, y.toDouble).toInt
  private def pow(x: Byte, y: Long): Int = math.pow(x.toDouble, y.toDouble).toInt
  private def pow(x: Byte, y: Short): Int = math.pow(x.toDouble, y.toDouble).toInt

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {

    def calculateWatts(primitive: Data.Primitive): Data.Primitive = {
      wattCounter.memoryUsage(primitive.volume.toLong)
      primitive
    }

    def calculateWattsForBigInt(x: scala.BigInt, y: Int): Long = {
      (scala.BigInt(x.bitLength) * y.abs / 8 + 1).toLong //FIXME overflow
    }
    wattCounter.cpuUsage(CpuArithmetic)
    val a = memory.pop()
    val b = memory.pop()

    val res = a match {
      case Int32(lhs) =>
        b match {
          case Int8(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case Int16(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case Int32(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case Uint8(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case Uint16(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case Uint32(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case BigInt(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs.intValue())))
          case Number(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case _ => throw VmErrorException(WrongType)
        }
      case Int16(lhs) =>
        b match {
          case Int8(rhs) =>
            calculateWatts(Int16(pow(lhs, rhs)))
          case Int16(rhs) =>
            calculateWatts(Int16(pow(lhs, rhs)))
          case Int32(rhs) =>
            calculateWatts(Int16(pow(lhs, rhs)))
          case Uint8(rhs) =>
            calculateWatts(Int16(pow(lhs, rhs)))
          case Uint16(rhs) =>
            calculateWatts(Int16(pow(lhs, rhs)))
          case Uint32(rhs) =>
            calculateWatts(Int16(pow(lhs, rhs)))
          case BigInt(rhs) =>
            calculateWatts(Int16(pow(lhs, rhs.intValue())))
          case Number(rhs) =>
            calculateWatts(Int16(pow(lhs, rhs)))
          case _ => throw VmErrorException(WrongType)
        }
      case Int8(lhs) =>
        b match {
          case Int8(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case Int16(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case Int32(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case Uint8(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case Uint16(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case Uint32(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs)))
          case BigInt(rhs) =>
            calculateWatts(Int32(pow(lhs, rhs.intValue())))
          case Number(rhs) =>
            calculateWatts(Number(pow(lhs, rhs).toDouble))
          case _ => throw VmErrorException(WrongType)
        }
      case Uint8(lhs) =>
        b match {
          case Int8(rhs) =>
            calculateWatts(Uint8(pow(lhs, rhs)))
          case Int16(rhs) =>
            calculateWatts(Uint8(pow(lhs, rhs)))
          case Int32(rhs) =>
            calculateWatts(Uint8(pow(lhs, rhs)))
          case Uint8(rhs) =>
            calculateWatts(Uint8(pow(lhs, rhs)))
          case Uint16(rhs) =>
            calculateWatts(Uint8(pow(lhs, rhs)))
          case Uint32(rhs) =>
            calculateWatts(Uint8(pow(lhs, rhs)))
          case BigInt(rhs) =>
            calculateWatts(Uint8(pow(lhs, rhs.byteValue())))
          case Number(rhs) =>
            calculateWatts(Uint8(pow(lhs, rhs)))
          case _ => throw VmErrorException(WrongType)
        }
      case Uint16(lhs) =>
        b match {
          case Int8(rhs) =>
            calculateWatts(Uint16(pow(lhs, rhs)))
          case Int16(rhs) =>
            calculateWatts(Uint16(pow(lhs, rhs)))
          case Int32(rhs) =>
            calculateWatts(Uint16(pow(lhs, rhs)))
          case Uint8(rhs) =>
            calculateWatts(Uint16(pow(lhs, rhs)))
          case Uint16(rhs) =>
            calculateWatts(Uint16(pow(lhs, rhs)))
          case Uint32(rhs) =>
            calculateWatts(Uint16(pow(lhs, rhs)))
          case BigInt(rhs) =>
            calculateWatts(Uint16(pow(lhs, rhs.intValue())))
          case Number(rhs) =>
            calculateWatts(Uint16(pow(lhs, rhs)))
          case _ => throw VmErrorException(WrongType)
        }
      case Uint32(lhs) =>
        b match {
          case Int8(rhs) =>
            calculateWatts(Uint32(pow(lhs, rhs)))
          case Int16(rhs) =>
            calculateWatts(Uint32(pow(lhs, rhs)))
          case Int32(rhs) =>
            calculateWatts(Uint32(pow(lhs, rhs)))
          case Uint8(rhs) =>
            calculateWatts(Uint32(pow(lhs, rhs)))
          case Uint16(rhs) =>
            calculateWatts(Uint32(pow(lhs, rhs)))
          case Uint32(rhs) =>
            calculateWatts(Uint32(pow(lhs, rhs)))
          case BigInt(rhs) =>
            calculateWatts(Uint32(pow(lhs, rhs.intValue())))
          case Number(rhs) =>
            calculateWatts(Uint32(pow(lhs, rhs)))
          case _ => throw VmErrorException(WrongType)
        }
      case Number(lhs) =>
        b match {
          case Int8(rhs) =>
            calculateWatts(Number(pow(lhs, rhs)))
          case Int16(rhs) =>
            calculateWatts(Number(pow(lhs, rhs)))
          case Int32(rhs) =>
            calculateWatts(Number(pow(lhs, rhs)))
          case Uint8(rhs) =>
            calculateWatts(Number(pow(lhs, rhs)))
          case Uint16(rhs) =>
            calculateWatts(Number(pow(lhs, rhs)))
          case Uint32(rhs) =>
            calculateWatts(Number(pow(lhs, rhs)))
          case BigInt(rhs) =>
            calculateWatts(Number(pow(lhs, rhs.longValue())))
          case Number(rhs) =>
            calculateWatts(Number(pow(lhs, rhs)))
          case _ => throw VmErrorException(WrongType)
        }
      case BigInt(lhs) =>
        b match {
          case Int8(rhs) =>
            wattCounter.memoryUsage(calculateWattsForBigInt(lhs, rhs.intValue()))
            BigInt(lhs.pow(rhs.toInt))
          case Int16(rhs) =>
            wattCounter.memoryUsage(calculateWattsForBigInt(lhs, rhs.intValue))
            BigInt(lhs.pow(rhs.toInt))
          case Int32(rhs) =>
            wattCounter.memoryUsage(calculateWattsForBigInt(lhs, rhs.intValue))
            BigInt(lhs.pow(rhs))
          case Uint8(rhs) =>
            wattCounter.memoryUsage(calculateWattsForBigInt(lhs, rhs.intValue))
            BigInt(lhs.pow(rhs))
          case Uint16(rhs) =>
            wattCounter.memoryUsage(calculateWattsForBigInt(lhs, rhs.intValue))
            BigInt(lhs.pow(rhs))
          case Uint32(rhs) =>
            wattCounter.memoryUsage(calculateWattsForBigInt(lhs, rhs.intValue))
            BigInt(lhs.pow(rhs.toInt))
          case BigInt(rhs) =>
            wattCounter.memoryUsage(calculateWattsForBigInt(lhs, rhs.intValue()))
            BigInt(lhs.pow(rhs.intValue()))
          case Number(rhs) =>
            wattCounter.memoryUsage(calculateWattsForBigInt(lhs, rhs.intValue))
            BigInt(lhs.pow(rhs.toInt))
          case _ => throw VmErrorException(WrongType)
        }
      case _ => throw VmErrorException(WrongType)
    }

    memory.push(res)
  }
}
