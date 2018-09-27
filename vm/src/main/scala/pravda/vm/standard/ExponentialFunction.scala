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

import pravda.vm.Data.Primitive.{BigInt, Int16, Int32, Int8, Number, Uint16, Uint32, Uint8}
import pravda.vm.Data.Type
import pravda.vm.VmError.WrongType
import pravda.vm.WattCounter.CpuArithmetic
import pravda.vm._

object ExponentialFunction extends FunctionDefinition {
  override def id: Long = 0x03L

  override def description: String = "Calculate exponential operation first two items in the stack and push result into"

  override def args: Seq[(String, Seq[Type])] = Seq(
    "x" -> Seq(Data.Type.Int32),
    "y" -> Seq(Data.Type.Int32)
  )

  override val returns = Seq(Data.Type.Number)
  private val div: Double = math.log10(2.0)
  override def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    implicit class DoubleExp(x: Double) {
      def **(y: Double): Double = math.pow(x, y)
      def **(y: Byte): Double = math.pow(x, y.toDouble)
      def **(y: Int): Double = math.pow(x, y.toDouble)
      def **(y: Long): Double = math.pow(x, y.toDouble)
      def **(y: Short): Double = math.pow(x, y.toDouble)
    }
    implicit class LongExp(x: Long) {
      def **(y: Double): Long = math.pow(x.toDouble, y).toLong
      def **(y: Byte): Long = math.pow(x.toDouble, y.toDouble).toLong
      def **(y: Int): Long = math.pow(x.toDouble, y.toDouble).toLong
      def **(y: Long): Long = math.pow(x.toDouble, y.toDouble).toLong
      def **(y: Short): Long = math.pow(x.toDouble, y.toDouble).toLong
    }
    implicit class IntExp(x: Int) {
      def **(y: Double): Int = math.pow(x.toDouble, y).toInt
      def **(y: Byte): Int = math.pow(x.toDouble, y.toDouble).toInt
      def **(y: Int): Int = math.pow(x.toDouble, y.toDouble).toInt
      def **(y: Long): Int = math.pow(x.toDouble, y.toDouble).toInt
      def **(y: Short): Int = math.pow(x.toDouble, y.toDouble).toInt
    }
    implicit class ShortExp(x: Short) {
      def **(y: Double): Short = math.pow(x.toDouble, y).toShort
      def **(y: Byte): Short = math.pow(x.toDouble, y.toDouble).toShort
      def **(y: Int): Short = math.pow(x.toDouble, y.toDouble).toShort
      def **(y: Long): Short = math.pow(x.toDouble, y.toDouble).toShort
      def **(y: Short): Short = math.pow(x.toDouble, y.toDouble).toShort
    }
    implicit class ByteExp(x: Byte) {
      def **(y: Double): Int = math.pow(x.toDouble, y).toInt
      def **(y: Byte): Int = math.pow(x.toDouble, y.toDouble).toInt
      def **(y: Int): Int = math.pow(x.toDouble, y.toDouble).toInt
      def **(y: Long): Int = math.pow(x.toDouble, y.toDouble).toInt
      def **(y: Short): Int = math.pow(x.toDouble, y.toDouble).toInt
    }

    def getCost(x: Double, y: Double): Long = {
      (math.log10(math.abs(x) + 1) / div * math.abs(y) / 8).toLong + 1
    }

    def getBigIntCost(x: scala.BigInt, y: Int): Long = {
      (x.abs.toString().length / div * math.abs(y) / 8).toLong + 1
    }
    wattCounter.cpuUsage(CpuArithmetic)
    val a = memory.pop()
    val b = memory.pop()

    wattCounter.cpuUsage(CpuArithmetic)

    val res = a match {
      case Int32(lhs) =>
        b match {
          case Int8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs)
          case Int16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs)
          case Int32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))

            Int32(lhs ** rhs)
          case Uint8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs)
          case Uint16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs)
          case Uint32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs)
          case BigInt(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.doubleValue()))
            Int32(lhs ** rhs.doubleValue())
          case Number(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs)
          case _ => throw VmErrorException(WrongType)
        }
      case Int16(lhs) =>
        b match {
          case Int8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int16(lhs ** rhs)
          case Int16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int16(lhs ** rhs)
          case Int32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int16(lhs ** rhs)
          case Uint8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int16(lhs ** rhs)
          case Uint16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int16((lhs ** rhs.toDouble).toShort)
          case Uint32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int16((lhs ** rhs.toDouble).toShort)
          case BigInt(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int16((lhs ** rhs.toDouble).toShort)
          case Number(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int16((lhs ** rhs).toShort)
          case _ => throw VmErrorException(WrongType)
        }
      case Int8(lhs) =>
        b match {
          case Int8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs)
          case Int16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs)
          case Int32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs)
          case Uint8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs)
          case Uint16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs)
          case Uint32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs)
          case BigInt(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Int32(lhs ** rhs.doubleValue())
          case Number(rhs) => Number((lhs ** rhs).toDouble)
          case _           => throw VmErrorException(WrongType)
        }
      case Uint8(lhs) =>
        b match {
          case Int8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint8(lhs ** rhs)
          case Int16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint8(lhs ** rhs)
          case Int32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint8(lhs ** rhs)
          case Uint8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint8(lhs ** rhs)
          case Uint16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint8(lhs ** rhs)
          case Uint32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint8(lhs ** rhs)
          case BigInt(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint8(lhs ** rhs.doubleValue())
          case Number(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint8(lhs ** rhs)
          case _ => throw VmErrorException(WrongType)
        }
      case Uint16(lhs) =>
        b match {
          case Int8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint16(lhs ** rhs)
          case Int16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint16(lhs ** rhs)
          case Int32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint16(lhs ** rhs)
          case Uint8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint16(lhs ** rhs)
          case Uint16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint16(lhs ** rhs)
          case Uint32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint16(lhs ** rhs)
          case BigInt(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint16(lhs ** rhs.doubleValue())
          case Number(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint16(lhs ** rhs)
          case _ => throw VmErrorException(WrongType)
        }
      case Uint32(lhs) =>
        b match {
          case Int8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint32(lhs ** rhs)
          case Int16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint32(lhs ** rhs)
          case Int32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint32(lhs ** rhs)
          case Uint8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint32(lhs ** rhs)
          case Uint16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint32(lhs ** rhs)
          case Uint32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint32(lhs ** rhs)
          case BigInt(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint32(lhs ** rhs.doubleValue())
          case Number(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Uint32(lhs ** rhs)
          case _ => throw VmErrorException(WrongType)
        }
      case Number(lhs) =>
        b match {
          case Int8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Number(lhs ** rhs)
          case Int16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Number(lhs ** rhs)
          case Int32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Number(lhs ** rhs)
          case Uint8(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Number(lhs ** rhs)
          case Uint16(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Number(lhs ** rhs)
          case Uint32(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Number(lhs ** rhs)
          case BigInt(rhs) =>
            wattCounter.memoryUsage(getCost(lhs.toDouble, rhs.toDouble))
            Number(lhs ** rhs.doubleValue())
          case Number(rhs) =>
            wattCounter.memoryUsage(getCost(lhs, rhs))
            Number(lhs ** rhs)
          case _ => throw VmErrorException(WrongType)
        }
      case BigInt(lhs) =>
        b match {
          case Int8(rhs) =>
            wattCounter.memoryUsage(getBigIntCost(lhs, rhs.intValue()))
            BigInt(lhs.pow(rhs.toInt))
          case Int16(rhs) =>
            wattCounter.memoryUsage(getBigIntCost(lhs, rhs.intValue))
            BigInt(lhs.pow(rhs.toInt))
          case Int32(rhs) =>
            wattCounter.memoryUsage(getBigIntCost(lhs, rhs.intValue))
            BigInt(lhs.pow(rhs))
          case Uint8(rhs) =>
            wattCounter.memoryUsage(getBigIntCost(lhs, rhs.intValue))
            BigInt(lhs.pow(rhs))
          case Uint16(rhs) =>
            wattCounter.memoryUsage(getBigIntCost(lhs, rhs.intValue))
            BigInt(lhs.pow(rhs))
          case Uint32(rhs) =>
            wattCounter.memoryUsage(getBigIntCost(lhs, rhs.intValue))
            BigInt(lhs.pow(rhs.toInt))
          case BigInt(rhs) =>
            wattCounter.memoryUsage(getBigIntCost(lhs, rhs.intValue()))
            BigInt(lhs.pow(rhs.intValue()))
          case Number(rhs) =>
            wattCounter.memoryUsage(getBigIntCost(lhs, rhs.intValue))
            BigInt(lhs.pow(rhs.toInt))
          case _ => throw VmErrorException(WrongType)
        }
      case _ => throw VmErrorException(WrongType)
    }

    memory.push(res)
  }
}
