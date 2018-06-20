package pravda.vm.operations

import pravda.vm.state.{Data, Memory, VmError, VmErrorException}
import pravda.vm.watt.WattCounter

/**
  * Pravda VM heap pravda.vm.Opcodes implementation.
  * @see [[pravda.vm.Opcodes]]
  * @param memory Access to VM memory
  */
final class HeapOperations(memory: Memory, wattCounter: WattCounter) {

  /**
    * Puts top item from the stack to the memory.
    * Pushes reference to the stack.
    * @see [[pravda.vm.Opcodes.MPUT]]
    */
  def mput(): Unit = {
    val data = memory.pop()
    val i = memory.heapPut(data)
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(Data.Primitive.Ref(i))
  }

  /**
    * Uses top item from the stack as reference
    * to data in the memory of program. Pushes
    * retrieved data to the stack.
    * @see [[pravda.vm.Opcodes.MGET]]
    */
  def mget(): Unit = {
    val i = ref(memory.pop())
    memory.heapGet(i.data) match {
      case primitive: Data.Primitive => memory.push(primitive)
      case _ => throw VmErrorException(VmError.WrongType)
    }
  }
}
