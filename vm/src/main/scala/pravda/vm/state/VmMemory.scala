package pravda.vm

package state

import pravda.vm.state.VmError.{StackUnderflow, WrongHeapIndex, WrongStackIndex}

import scala.collection.mutable.ArrayBuffer

final case class VmMemory(
    stack: ArrayBuffer[Data],
    heap: ArrayBuffer[Data]
) extends Memory {

  private val limits = new ArrayBuffer[Int](1024)

  def limit(index: Int): Unit = {
    if (index < 0) {
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

  private def currentLimit = if (limits.isEmpty) 0 else limits.last

  def pop(): Data = {
    if (stack.size <= currentLimit) {
      throw VmErrorException(StackUnderflow)
    }
    stack.remove(stack.length - 1)
  }

  def top(): Data = {
    if (stack.size <= currentLimit) {
      throw VmErrorException(StackUnderflow)
    }
    stack(stack.length - 1)
  }

  def top(n: Int): Seq[Data] = {
    if (stack.size <= currentLimit - n) {
      throw VmErrorException(StackUnderflow)
    }
    stack.slice(stack.length - n, stack.length)
  }

  def push(x: Seq[Data]): Unit = {
    stack ++= x
  }

  def push(x: Data): Unit = {
    stack += x
  }

  def get(i: Int): Data = {
    if (i < currentLimit || i >= stack.length) {
      throw VmErrorException(WrongStackIndex)
    }
    stack(i)
  }

  def clear(): Unit = {
    stack.remove(currentLimit, stack.length - currentLimit)
  }

  def all: Seq[Data] = {
    stack.takeRight(stack.length - currentLimit)
  }

  def swap(i: Int, j: Int): Unit = {
    if (i < currentLimit || j < currentLimit || i >= stack.length || j >= stack.length) {
      throw VmErrorException(WrongStackIndex)
    }
    val f = stack(i)
    stack(i) = stack(j)
    stack(j) = f
  }

  def length: Int = stack.length

  def heapPut(x: Data): Int = {
    heap += x
    heap.length - 1
  }

  def heapGet(idx: Int): Data = {
    if (idx >= heap.length || idx < 0) {
      throw VmErrorException(WrongHeapIndex)
    }
    heap(idx)
  }

  def heapLength: Int = heap.length

}

object VmMemory {

  def empty: VmMemory = new VmMemory(
    stack = new ArrayBuffer[Data](1024),
    heap = new ArrayBuffer[Data](1024)
  )

}
