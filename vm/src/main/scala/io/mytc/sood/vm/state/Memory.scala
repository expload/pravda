package io.mytc.sood.vm
package state

import scala.collection.mutable.ArrayBuffer

case class Memory(
  stack: ArrayBuffer[Data],
  heap: ArrayBuffer[Data]
) {
  def pop(): Data =
    stack.remove(stack.length - 1)

  def push(x: Data): stack.type =
    stack += x

}

object Memory {

  def empty: Memory = new Memory(
    stack = new ArrayBuffer[Data](1024),
    heap = new ArrayBuffer[Data](1024)
  )

}
