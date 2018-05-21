package io.mytc.sood.vm

package state

import io.mytc.sood.vm.state.VmError.StackUnderflow

import scala.collection.mutable.ArrayBuffer

final case class Memory(
    stack: ArrayBuffer[Data],
    heap: ArrayBuffer[Data]
) {

  def pop(): Data = {
    if (stack.isEmpty) {
      throw VmErrorException(StackUnderflow)
    }

    stack.remove(stack.length - 1)
  }

  def push(x: Data): Unit =
    stack += x

  def top(num: Int): Memory = {
    if (stack.length < num) {
      throw VmErrorException(StackUnderflow)
    }

    val topStack = stack.takeRight(num)
    stack.remove(stack.length - num, num)
    Memory(topStack, heap.clone())
  }

  def ++=(other: Memory): Unit = {
    stack ++= other.stack
    heap ++= other.heap.drop(heap.length)
  }

}

object Memory {

  def empty: Memory = new Memory(
    stack = new ArrayBuffer[Data](1024),
    heap = new ArrayBuffer[Data](1024)
  )

}
