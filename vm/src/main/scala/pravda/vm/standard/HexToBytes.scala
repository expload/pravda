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

object HexToBytes extends FunctionDefinition {

  val id = 0x04L

  val description =
    "Takes hex string from stack, pushes bytes to the stack"

  val args: Seq[(String, Seq[Type])] = Seq(
    "hex" -> Seq(Data.Type.Utf8),
  )

  val returns = Seq(Data.Type.Bytes)

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    val hex = utf8(memory.pop())
    wattCounter.cpuUsage(CpuArithmetic * hex.length)
    wattCounter.memoryUsage(hex.length.toLong * 2)

    val result = try {
      pravda.common.bytes.hex2byteString(hex)
    } catch {
      case _: Throwable =>
        throw ThrowableVmError(Error.InvalidArgument)
    }

    memory.push(Data.Primitive.Bytes(result))
  }
}
