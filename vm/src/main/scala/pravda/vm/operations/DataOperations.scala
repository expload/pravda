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

import pravda.vm.Data._
import pravda.vm.Error.WrongType
import pravda.vm.operations.annotation.OpcodeImplementation
import pravda.vm.{Memory, Opcodes, ThrowableVmError, WattCounter}

final class DataOperations(memory: Memory, wattCounter: WattCounter) {

  import Primitive._

  @OpcodeImplementation(
    opcode = Opcodes.CAST,
    description = "Casts primitive to another type."
  )
  def cast(): Unit = {
    val `type` = integer(memory.pop())
    val data = memory.pop()
    val result = data.cast(Type @@ `type`.toByte)
    wattCounter.cpuUsage(WattCounter.CpuArithmetic)
    memory.push(result)
  }

  @OpcodeImplementation(
    opcode = pravda.vm.Opcodes.SLICE,
    description =
      "Takes start index, end index and item from the stack. Makes slice of item and puts result to the stack."
  )
  def slice(): Unit = {
    val from = integer(memory.pop()).toInt
    val until = integer(memory.pop()).toInt
    val sliced = memory.pop() match {
      case Utf8(data)  => Utf8(data.substring(from, until))
      case Bytes(data) => Bytes(data.substring(from, until))
      case _           => throw ThrowableVmError(WrongType)
    }
    wattCounter.cpuUsage(WattCounter.CpuArithmetic)
    wattCounter.memoryUsage(sliced.volume.toLong)
    memory.push(sliced)
  }

  @OpcodeImplementation(
    opcode = pravda.vm.Opcodes.CONCAT,
    description = "Takes two items from stack. Concatenates them and put result to stack."
  )
  def concat(): Unit = {
    val data = (memory.pop(), memory.pop()) match {
      case (Utf8(a), Utf8(b))   => Utf8(a + b)
      case (Bytes(a), Bytes(b)) => Bytes(a.concat(b))
      case _                    => throw ThrowableVmError(WrongType)
    }
    wattCounter.cpuUsage(WattCounter.CpuArithmetic)
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(data)
  }

}
