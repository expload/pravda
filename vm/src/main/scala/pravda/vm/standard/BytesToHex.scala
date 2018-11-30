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

import pravda.vm.Data.Type
import pravda.vm.WattCounter.CpuArithmetic
import pravda.vm._
import pravda.vm.operations._

object BytesToHex extends FunctionDefinition {

  val id = 0x05L

  val description =
    "Takes bytes from stack, pushes hex string to the stack"

  val args: Seq[(String, Seq[Type])] = Seq(
    "bytes" -> Seq(Data.Type.Bytes),
  )

  val returns = Seq(Data.Type.Utf8)

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    val b = bytes(memory.pop())
    wattCounter.cpuUsage(CpuArithmetic * b.size())
    wattCounter.memoryUsage(b.size().toLong * 2)

    val result = try {
      pravda.common.bytes.bytes2hex(b.toByteArray)
    } catch {
      case _: Throwable =>
        throw ThrowableVmError(Error.InvalidArgument)
    }

    memory.push(Data.Primitive.Utf8(result))
  }
}
