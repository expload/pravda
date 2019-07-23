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

import pravda.common.vm.Data.Array.Int8Array
import pravda.common.vm.Data.Type
import pravda.common.vm.Error.{InvalidArgument, WrongType}
import pravda.common.vm._
import pravda.vm.WattCounter.CpuArithmetic
import pravda.vm._
import pravda.vm.operations._

object WriteSliceByteArray extends FunctionDefinition {

  val id = 0x07L

  val description =
    "Takes byte array, index from stack, bytes to write. " +
      "Writes given bytes from given index in the given array. " +
      "Returns reference to array"

  val args: Seq[(String, Seq[Type])] = Seq(
    "bytes" -> Seq(Data.Type.Bytes),
    "index" -> Seq(Data.Type.BigInt),
    "array" -> Seq(Data.Type.Array)
  )

  val returns = Seq(Data.Type.Array)

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    val r = ref(memory.pop())
    val res = memory.heapGet(r) match {
      case Int8Array(data) =>
        val ind = memory.pop() match {
          case Data.Primitive.BigInt(b) =>
            if (b < 0) throw ThrowableVmError(InvalidArgument)
            else if (b > data.length) throw ThrowableVmError(InvalidArgument)
            else b.toInt
          case _ => throw ThrowableVmError(WrongType)
        }

        val bytes = memory.pop() match {
          case Data.Primitive.Bytes(b) =>
            if (ind + b.size() > data.length) throw ThrowableVmError(InvalidArgument)
            else b
          case _ => throw ThrowableVmError(WrongType)
        }

        bytes.toByteArray.zipWithIndex.foreach {
          case (b, i) => data(ind + i) = b
        }

        wattCounter.cpuUsage(bytes.size() * CpuArithmetic)

        r

      case _ => throw ThrowableVmError(WrongType)
    }

    memory.push(res)
  }
}
