package pravda.vm.operations

import java.nio.ByteBuffer

import pravda.vm.VmError.{CallStackOverflow, CallStackUnderflow}
import pravda.vm.WattCounter.{CpuProgControl, CpuSimpleArithmetic}
import pravda.vm.operations.annotation.OpcodeImplementation
import pravda.vm.{Memory, Opcodes, VmErrorException, WattCounter}

import scala.collection.mutable

final class ControlOperations(program: ByteBuffer,
                              callStack: mutable.Buffer[Int],
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
    if (callStack.size > 1024) {
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
    if (callStack.isEmpty) {
      throw VmErrorException(CallStackUnderflow)
    }
    val offset = callStack.remove(callStack.length - 1)
    program.position(offset)
  }
}
