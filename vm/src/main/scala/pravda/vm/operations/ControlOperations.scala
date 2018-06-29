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
    val offset = ref(memory.pop())
    val condition = boolean(memory.pop())
    if (condition) {
      program.position(offset.data)
    }
  }

  def jump(): Unit = {
    wattCounter.cpuUsage(CpuProgControl)
    program.position(ref(memory.pop()).data)
  }

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

  def ret(): Unit = {
    if (callStack.isEmpty) {
      throw VmErrorException(CallStackUnderflow)
    }
    val offset = callStack.remove(callStack.length - 1)
    program.position(offset)
  }
}
