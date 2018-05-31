package pravda.vm

package state

import pravda.vm.state.VmError.{StackUnderflow, WrongHeapIndex, WrongStackIndex}
import pravda.vm.watt.WattCounter

import scala.collection.mutable.ArrayBuffer

final case class Memory(
    stack: ArrayBuffer[Data],
    heap: ArrayBuffer[Data],
    wattCounter: WattCounter
) {
  private var stackVolume = stack.map(_.size()).sum
  private var heapVolume = heap.map(_.size()).sum

  def volume: Long = stackVolume + heapVolume

  private var maxStackVolume = stackVolume
  private var maxHeapVolume = heapVolume

  def maxVolume: Long = maxStackVolume + maxHeapVolume

  private var limits = ArrayBuffer.empty[Int]

  def limit(index: Int): Unit = {
    if(index < 0) {
      limits += currentLimit
    } else if (stack.length - currentLimit < index) {
      throw VmErrorException(StackUnderflow)
    } else {
      limits += (stack.length - index)
    }
  }

  def dropLimit(): Unit = {
    limits.remove(limits.length - 1)
  }

  private def currentLimit = if(limits.isEmpty) 0 else limits.last

  def pop(): Data = {
    if (stack.size <= currentLimit) {
      throw VmErrorException(StackUnderflow)
    }
    stackVolume -= stack(stack.length - 1).size()
    stack.remove(stack.length - 1)
  }

  def push(x: Data): Unit = {
    stackVolume += x.size()
    if(stackVolume > maxStackVolume) {
      maxStackVolume = stackVolume
      wattCounter.memoryUsage(maxVolume)
    }
    stack += x
  }

  def get(i: Int): Data = {
    if(i < currentLimit || i >= stack.length) {
      throw VmErrorException(WrongStackIndex)
    }
    stack(i)
  }

  def swap(i: Int, j: Int): Unit = {
    if(i < currentLimit || j < currentLimit || i >= stack.length || j >= stack.length) {
      throw VmErrorException(WrongStackIndex)
    }
    val f = stack(i)
    stack(i) = stack(j)
    stack(j) = f
  }

  def length: Int = stack.length

  def heapPut(x: Data): Int = {
    heapVolume += x.size()
    if(heapVolume > maxHeapVolume) {
      maxHeapVolume = heapVolume
      wattCounter.memoryUsage(maxVolume)
    }
    heap += x
    heap.length - 1
  }

  def heapGet(idx: Int): Data = {
    if(idx >= heap.length || idx < 0) {
      throw VmErrorException(WrongHeapIndex)
    }
    heap(idx)
  }

  def heapLength: Int = heap.length
}

object Memory {

  def empty(wattCounter: WattCounter): Memory = new Memory(
    stack = new ArrayBuffer[Data](1024),
    heap = new ArrayBuffer[Data](1024),
    wattCounter = wattCounter
  )

}
