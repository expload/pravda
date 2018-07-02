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
    description = "Pushes the word following the opcode to the stack."
  )
  def push(): Unit = {
    Data.readFromByteBuffer(program) match {
      case data: Data.Primitive =>
        wattCounter.memoryUsage(data.volume.toLong)
        memory.push(data)
      case _ => throw VmErrorException(VmError.WrongType)
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
