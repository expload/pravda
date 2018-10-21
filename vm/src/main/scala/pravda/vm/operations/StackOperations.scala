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

import java.nio.ByteBuffer

import pravda.vm._
import pravda.vm.operations.annotation.OpcodeImplementation

/**
  * Pravda VM stack control pravda.vm.Opcodes implementation.
  * @see pravda.vm.Opcodes
  * @param memory Access to VM memory
  */
final class StackOperations(memory: Memory, program: ByteBuffer, wattCounter: WattCounter) {

  @OpcodeImplementation(
    opcode = Opcodes.POP,
    description = "Removes first item from the stack."
  )
  def pop(): Unit = {
    memory.pop()
  }

  @OpcodeImplementation(
    opcode = Opcodes.PUSHX,
    description = "Pushes the data primitive following the opcode to the stack. Refs are prohibited"
  )
  def push(): Unit = {
    Data.readFromByteBuffer(program) match {
      case _: Data.Primitive.Ref =>
        throw ThrowableVmError(Error.WrongType)
      case data: Data.Primitive =>
        wattCounter.memoryUsage(data.volume.toLong)
        memory.push(data)
      case _ => throw ThrowableVmError(Error.WrongType)
    }
  }

  @OpcodeImplementation(
    opcode = Opcodes.DUP,
    description = "Duplicates first item of the stack."
  )
  def dup(): Unit = {
    val x = memory.pop()
    wattCounter.memoryUsage(x.volume.toLong)
    memory.push(x)
    memory.push(x)
  }

  @OpcodeImplementation(
    opcode = Opcodes.DUPN,
    description = "Duplicates `(n+1)`-th item of the stack " +
      "where `n` is the first item in stack."
  )
  def dupN(): Unit = {
    val n = integer(memory.pop())
    val data = memory.get(memory.length - n.toInt)
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(data)
  }

  @OpcodeImplementation(
    opcode = Opcodes.SWAP,
    description = "Swaps first two items in the stack."
  )
  def swap(): Unit = {
    val fsti = memory.length - 1
    val sndi = fsti - 1
    memory.swap(fsti, sndi)
  }

  @OpcodeImplementation(
    opcode = Opcodes.SWAPN,
    description = "Swaps the second item of the stack with " +
      "the `(n+1)`-th item of the stack where `n` is first item in the stack."
  )
  def swapN(): Unit = {
    val n = integer(memory.pop())
    val fsti = memory.length - 1
    val sndi = memory.length - n
    memory.swap(fsti, sndi.toInt)
  }
}
