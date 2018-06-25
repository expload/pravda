package pravda.vm.operations

import java.nio.ByteBuffer

import pravda.vm._

/**
  * Pravda VM stack control pravda.vm.Opcodes implementation.
  * @see [[pravda.vm.Opcodes]]
  * @param memory Access to VM memory
  */
final class StackOperations(memory: Memory, program: ByteBuffer, wattCounter: WattCounter) {

  def push(): Unit = {
    Data.readFromByteBuffer(program) match {
      case data: Data.Primitive =>
        wattCounter.memoryUsage(data.volume.toLong)
        memory.push(data)
      case _ => throw VmErrorException(VmError.WrongType)
    }
  }

  def dup(): Unit = {
    val x = memory.pop()
    wattCounter.memoryUsage(x.volume.toLong)
    memory.push(x)
    memory.push(x)
  }

  def dupN(): Unit = {
    val n = integer(memory.pop())
    val data = memory.get(memory.length - n.toInt)
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(data)
  }

  def swap(): Unit = {
    val fsti = memory.length - 1
    val sndi = fsti - 1
    memory.swap(fsti, sndi)
  }

  def swapN(): Unit = {
    val n = integer(memory.pop())
    val fsti = memory.length - 1
    val sndi = memory.length - n
    memory.swap(fsti, sndi.toInt)
  }
}
