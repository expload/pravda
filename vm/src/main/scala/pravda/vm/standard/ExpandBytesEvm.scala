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
import pravda.common.vm.Data.Type
import pravda.common.vm.Error.InvalidArgument
import pravda.common.vm._
import pravda.vm.WattCounter.CpuArithmetic
import pravda.vm._
import pravda.vm.operations._

object ExpandBytesEvm extends FunctionDefinition {

  val id = 0x08L

  val description =
    "Takes bytes from stack. Return expanded to 32 length bytes. "

  val args: Seq[(String, Seq[Type])] = Seq(
    "bytes" -> Seq(Data.Type.Bytes)
  )

  val returns = Seq(Data.Type.Bytes)

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    val bs = bytes(memory.pop())
    val res = if (bs.size() > 32) {
      throw ThrowableVmError(InvalidArgument)
    } else {
      Data.Primitive.Bytes(bs.concat(ByteString.copyFrom(Array.fill[Byte](32 - bs.size())(0))))
    }

    wattCounter.cpuUsage(CpuArithmetic)
    wattCounter.memoryUsage(32L)

    memory.push(res)
  }
}
