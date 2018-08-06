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

import pravda.vm.VmError.{CallStackOverflow, CallStackUnderflow}
import pravda.vm.WattCounter.{CpuProgControl, CpuSimpleArithmetic}
import pravda.vm.operations.annotation.OpcodeImplementation
import pravda.vm._

import scala.collection.mutable

final class ControlOperations(program: ByteBuffer,
                              callStack: mutable.Buffer[Int],
                              metas: mutable.Buffer[Meta],
                              callMetaStack: mutable.Buffer[List[Meta]],
                              memory: Memory,
                              wattCounter: WattCounter) {

  @OpcodeImplementation(
    opcode = Opcodes.JUMPI,
    description = "If boolean value in head of stack is true then " +
      "alters program execution counter to value of second item in the stack."
  )
  def jumpi(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic, CpuProgControl)
    val offset = ref(memory.pop())
    val condition = boolean(memory.pop())
    if (condition) {
      program.position(offset.data)
    }
  }

  @OpcodeImplementation(
    opcode = Opcodes.JUMP,
    description = "Alters program execution counter to value of first item of the stack."
  )
  def jump(): Unit = {
    wattCounter.cpuUsage(CpuProgControl)
    program.position(ref(memory.pop()).data)
  }

  @OpcodeImplementation(
    opcode = Opcodes.CALL,
    description = "Firstly, it pushes current program counter to the " +
      "separate stack (so called 'call stack'). Then it alters program " +
      "execution counter to the value of the first item of the stack."
  )
  def call(): Unit = {
    wattCounter.cpuUsage(CpuProgControl)
    val currentOffset = program.position()
    val callOffset = ref(memory.pop())
    callStack += currentOffset
    callMetaStack += metas.toList
    if (callStack.size > 1024 || callMetaStack.size > 1024) {
      throw VmErrorException(CallStackOverflow)
    }
    program.position(callOffset.data)
  }

  @OpcodeImplementation(
    opcode = Opcodes.RET,
    description = "Alters program execution counter to the " +
      "value of the first item of the call stack (see CALL opcode)."
  )
  def ret(): Unit = {
    if (callStack.isEmpty || callMetaStack.isEmpty) {
      throw VmErrorException(CallStackUnderflow)
    }
    val offset = callStack.remove(callStack.length - 1)
    callMetaStack.remove(callMetaStack.length - 1)
    program.position(offset)
  }
}
