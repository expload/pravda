/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.vm.impl

import pravda.common.domain.Address
import pravda.vm.Error.{CallStackOverflow, CallStackUnderflow}
import pravda.vm.{Data, Error, Memory, ThrowableVmError}

import scala.collection.mutable.ArrayBuffer

final case class MemoryImpl(stack: ArrayBuffer[Data.Primitive], heap: ArrayBuffer[Data]) extends Memory {

  val callStack = new ArrayBuffer[(Option[Address], ArrayBuffer[Int])](1024)

  private val limits = new ArrayBuffer[Int](1024)

  private def localCallStack = {
    if (callStack.isEmpty) {
      // Default (no saved program invoked) call stack.
      callStack += (None -> new ArrayBuffer[Int](1024))
    }
    callStack(callStack.length - 1)._2
  }

  def enterProgram(address: Address): Unit =
    callStack += (Some(address) -> new ArrayBuffer[Int](1024))

  def exitProgram(): Address =
    callStack.remove(callStack.length - 1) match {
      case (Some(address), _) => address
      case _                  => throw ThrowableVmError(Error.ExtCallStackUnderflow)
    }

  def popCall(): Int = {
    if (localCallStack.isEmpty) {
      throw ThrowableVmError(CallStackUnderflow)
    }
    localCallStack.remove(localCallStack.length - 1)
  }

  def pushCall(offset: Int): Unit = {
    if (localCallStack.length >= 1024) {
      throw ThrowableVmError(CallStackOverflow)
    }
    localCallStack += offset
  }

  def limit(index: Int): Unit = {
    if (index < 0) {
      limits += currentLimit
    } else if (stack.length - currentLimit < index) {
      throw ThrowableVmError(Error.StackUnderflow)
    } else {
      limits += (stack.length - index)
    }
  }

  def dropLimit(): Unit = {
    limits.remove(limits.length - 1)
  }

  private def currentLimit = if (limits.isEmpty) 0 else limits.last

  def pop(): Data.Primitive = {
    if (stack.size <= currentLimit) {
      throw ThrowableVmError(Error.StackUnderflow)
    }
    stack.remove(stack.length - 1)
  }

  def top(): Data.Primitive = {
    if (stack.size <= currentLimit) {
      throw ThrowableVmError(Error.StackUnderflow)
    }
    stack(stack.length - 1)
  }

  def top(n: Int): Seq[Data.Primitive] = {
    if (stack.size <= currentLimit - n) {
      throw ThrowableVmError(Error.StackUnderflow)
    }
    stack.slice(stack.length - n, stack.length)
  }

  def push(x: Data.Primitive): Unit = {
    stack += x
  }

  def get(i: Int): Data.Primitive = {
    if (i < currentLimit || i >= stack.length) {
      throw ThrowableVmError(Error.WrongStackIndex)
    }
    stack(i)
  }

  def clear(): Unit = {
    stack.remove(currentLimit, stack.length - currentLimit)
  }

  def all: Seq[Data.Primitive] = {
    stack.takeRight(stack.length - currentLimit)
  }

  def swap(i: Int, j: Int): Unit = {
    if (i < currentLimit || j < currentLimit || i >= stack.length || j >= stack.length) {
      throw ThrowableVmError(Error.WrongStackIndex)
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
      throw ThrowableVmError(Error.WrongHeapIndex)
    }
    heap(idx)
  }

  def heapLength: Int = heap.length

}

object MemoryImpl {

  def empty: MemoryImpl = new MemoryImpl(
    stack = new ArrayBuffer[Data.Primitive](1024),
    heap = new ArrayBuffer[Data](1024)
  )

}
