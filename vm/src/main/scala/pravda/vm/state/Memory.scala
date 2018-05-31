package pravda.vm

package state

import pravda.vm.state.VmError.StackUnderflow

import scala.collection.mutable.ArrayBuffer

final case class Memory(
    stack: ArrayBuffer[Data],
    heap: ArrayBuffer[Data]
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
    if(stackVolume > maxStackVolume) maxStackVolume = stackVolume
    stack += x
  }

  def get(i: Int): Data = {
    if(i < currentLimit) {
      // TODO: throw exception
    }
    stack(i)
  }

  def swap(i: Int, j: Int): Unit = {
    if(i < currentLimit || j < currentLimit) {
      // TODO: throw exception
    }
    val f = stack(i)
    stack(i) = stack(j)
    stack(j) = f
  }

  def length: Int = stack.length

  def heapPut(x: Data): Int = {
    heap += x
    heapVolume += x.size()
    if(heapVolume > maxHeapVolume) maxHeapVolume = heapVolume
    heap.length - 1
  }

  def heapGet(idx: Int): Data = {
    heap(idx)
  }

  def heapLength: Int = heap.length
}

object Memory {

  def empty: Memory = new Memory(
    stack = new ArrayBuffer[Data](1024),
    heap = new ArrayBuffer[Data](1024)
  )

}
