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

import com.google.protobuf.ByteString
import pravda.common.vm.Data.Array.Int8Array
import pravda.common.vm.Data.Type
import pravda.common.vm.Error.{InvalidArgument, WrongType}
import pravda.common.vm._
import pravda.vm.WattCounter.CpuArithmetic
import pravda.vm._
import pravda.vm.operations._

object SliceByteArray extends FunctionDefinition {

  val id = 0x06L

  val description =
    "Takes the byte array, index and size from the stack. Returns size bytes from the given index in the given array."

  val args: Seq[(String, Seq[Type])] = Seq(
    "size" -> Seq(Data.Type.BigInt),
    "index" -> Seq(Data.Type.BigInt),
    "array" -> Seq(Data.Type.Array)
  )

  val returns = Seq(Data.Type.Bytes)

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    val res = memory.heapGet(ref(memory.pop())) match {
      case Int8Array(data) =>
        val ind = memory.pop() match {
          case Data.Primitive.BigInt(b) =>
            if (b < 0) throw ThrowableVmError(InvalidArgument)
            else if (b > Int.MaxValue) Int.MaxValue
            else b.toInt
          case _ => throw ThrowableVmError(WrongType)
        }

        val size = memory.pop() match {
          case Data.Primitive.BigInt(b) =>
            if (b < 0) throw ThrowableVmError(InvalidArgument)
            else if (b > Int.MaxValue) Int.MaxValue
            else b.toInt
          case _ => throw ThrowableVmError(WrongType)
        }
        Data.Primitive.Bytes(ByteString.copyFrom(data.slice(ind, ind + size).toArray))
      case _ => throw ThrowableVmError(WrongType)
    }

    wattCounter.cpuUsage(CpuArithmetic)
    wattCounter.memoryUsage(res.data.size().toLong * 2)

    memory.push(res)
  }
}
