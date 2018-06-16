package pravda.vm.operations

import pravda.vm.state.Memory
import pravda.vm.watt.WattCounter

/**
  * Pravda VM stack control pravda.vm.Opcodes implementation.
  * @see [[pravda.vm.Opcodes]]
  * @param memory Access to VM memory
  */
final class StackOperations(memory: Memory, wattCounter: WattCounter) {

  def dup(): Unit = {
    val x = memory.pop()
    wattCounter.memoryUsage(x.volume.toLong)
    memory.push(x)
    memory.push(x)
  }

  def dupN(): Unit = {
    val n = int32(memory.pop())
    val data = memory.get(memory.length - n)
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(data)
  }

  def swap(): Unit = {
    val fsti = memory.length - 1
    val sndi = fsti - 1
    memory.swap(fsti, sndi)
  }

  def swapN(): Unit = {
    val n = int32(memory.pop())
    val fsti = memory.length - 1
    val sndi = memory.length - n
    memory.swap(fsti, sndi)
  }
}
