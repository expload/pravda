package pravda.vm.watt

import pravda.vm.state.{Data, Memory, VmMemory}

final case class MemoryWithCounter(
    memory: VmMemory,
    wattCounter: WattCounter
) extends Memory {

  private var stackVolume = stack.map(_.size()).sum.toLong
  private var heapVolume = heap.map(_.size()).sum.toLong
  private var maxStackVolume = stackVolume
  private var maxHeapVolume = heapVolume

  def volume: Long = stackVolume + heapVolume
  def maxVolume: Long = maxStackVolume + maxHeapVolume

  def stack: Seq[Data] = memory.stack
  def heap: Seq[Data] = memory.heap

  def limit(index: Int): Unit = memory.limit(index)

  def dropLimit(): Unit = memory.dropLimit()

  def top(): Data = memory.top()

  def top(n: Int): Seq[Data] = memory.top(n)

  def pop(): Data = {
    stackVolume -= memory.top().size()
    memory.pop()
  }

  def push(x: Seq[Data]): Unit = {
    stackVolume += x.map(_.size()).sum
    if (stackVolume > maxStackVolume) {
      maxStackVolume = stackVolume
      wattCounter.memoryUsage(maxVolume)
    }
    memory.push(x)
  }

  def push(x: Data): Unit = {
    stackVolume += x.size()
    if (stackVolume > maxStackVolume) {
      maxStackVolume = stackVolume
      wattCounter.memoryUsage(maxVolume)
    }
    memory.push(x)
  }

  def get(i: Int): Data = {
    memory.get(i)
  }

  def clear(): Unit = {
    stackVolume -= memory.all.map(_.size()).sum
    memory.clear()
  }

  def all: Seq[Data] = memory.all

  def swap(i: Int, j: Int): Unit = memory.swap(i, j)

  def length: Int = memory.length

  def heapPut(x: Data): Int = {
    heapVolume += x.size()
    if (heapVolume > maxHeapVolume) {
      maxHeapVolume = heapVolume
      wattCounter.memoryUsage(maxVolume)
    }
    memory.heapPut(x)
  }

  def heapGet(idx: Int): Data = {
    memory.heapGet(idx)
  }

  def heapLength: Int = memory.heapLength

}
