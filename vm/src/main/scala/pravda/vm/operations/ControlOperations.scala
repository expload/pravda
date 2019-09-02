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

import pravda.common.vm.Opcodes
import pravda.vm.WattCounter.{CpuProgControl, CpuSimpleArithmetic}
import pravda.vm._
import pravda.vm.operations.annotation.OpcodeImplementation

final class ControlOperations(program: ByteBuffer, memory: Memory, wattCounter: WattCounter) {

  @OpcodeImplementation(
    opcode = Opcodes.JUMPI,
    description = "If the boolean value in head of the stack is true, then " +
      "alters the program execution counter to the value of the second item in the stack."
  )
  def jumpi(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic, CpuProgControl)
    val i = offset(memory.pop())
    val condition = boolean(memory.pop())
    if (condition) {
      memory.setCounter(i)
      program.position(memory.currentCounter)
    }
  }

  @OpcodeImplementation(
    opcode = Opcodes.JUMP,
    description = "Alters the program execution counter to the value of the first item of the stack."
  )
  def jump(): Unit = {
    wattCounter.cpuUsage(CpuProgControl)
    val i = offset(memory.pop())
    memory.setCounter(i)
    program.position(memory.currentCounter)
  }

  @OpcodeImplementation(
    opcode = Opcodes.CALL,
    description = "Firstly, it pushes current program counter to a " +
      "separate stack (the so called 'call stack'). Then, it alters the program " +
      "execution counter to the value of the first item of the stack."
  )
  def call(): Unit = {
    wattCounter.cpuUsage(CpuProgControl)
    val callOffset = offset(memory.pop())
    memory.makeCall(callOffset)
    program.position(memory.currentCounter)
  }

  @OpcodeImplementation(
    opcode = Opcodes.RET,
    description = "Alters the program execution counter to the " +
      "value of the first item of the call stack (see CALL opcode)."
  )
  def ret(): Unit = {
    memory.makeRet()
    program.position(memory.currentCounter)
  }
}
