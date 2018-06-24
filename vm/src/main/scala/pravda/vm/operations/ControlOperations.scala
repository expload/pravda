package pravda.vm.operations

import java.nio.ByteBuffer

import pravda.vm.VmError.{CallStackOverflow, CallStackUnderflow}
import pravda.vm.WattCounter.{CpuProgControl, CpuSimpleArithmetic}
import pravda.vm.{Memory, VmErrorException, WattCounter}

import scala.collection.mutable

final class ControlOperations(program: ByteBuffer,
                              callStack: mutable.Buffer[Int],
                              memory: Memory,
                              wattCounter: WattCounter) {

  def jumpi(): Unit = {
    wattCounter.cpuUsage(CpuSimpleArithmetic, CpuProgControl)
    val offset = int32(memory.pop())
    val condition = boolean(memory.pop())
    if (condition) {
      program.position(offset)
    }
  }

  def jump(): Unit = {
    wattCounter.cpuUsage(CpuProgControl)
    program.position(int32(memory.pop()))
  }

  def call(): Unit = {
    wattCounter.cpuUsage(CpuProgControl)
    val currentOffset = program.position()
    val callOffset = int32(memory.pop())
    callStack += currentOffset
    if (callStack.size > 1024) {
      throw VmErrorException(CallStackOverflow)
    }
    program.position(callOffset)
  }

  def ret(): Unit = {
    if (callStack.isEmpty) {
      throw VmErrorException(CallStackUnderflow)
    }
    val offset = callStack.remove(callStack.length - 1)
    program.position(offset)
  }
}
