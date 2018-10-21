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
import pravda.vm.Error._
import pravda.vm.{Data, Error, Memory, ThrowableVmError}

import scala.collection.mutable.ArrayBuffer

final case class MemoryImpl(stack: ArrayBuffer[Data.Primitive], heap: ArrayBuffer[Data]) extends Memory {

  val callStack = new ArrayBuffer[(Option[Address], ArrayBuffer[Int])](1024)
  val pcallStack = new ArrayBuffer[Int](1024)

  private var offset = 0

  private val limits = new ArrayBuffer[Int](1024)

  def setCounter(newOffset: Int): Unit = {
    offset = newOffset
  }

  def currentCounter: Int = offset

  private def localCallStack = {
    if (callStack.isEmpty) {
      // Default (no saved program invoked) call stack.
      callStack += (None -> new ArrayBuffer[Int](1024))
    }
    callStack.last._2
  }

  def enterProgram(address: Address): Unit = {
    if (pcallStack.length >= 1024 || callStack.length >= 1024) {
      throw ThrowableVmError(ExtCallStackOverflow)
    }
    pcallStack += currentCounter
    callStack += (Some(address) -> new ArrayBuffer[Int](1024))
    setCounter(0)
  }

  def exitProgram(): Unit = {
    val lastOffset = if (pcallStack.nonEmpty) {
      pcallStack.remove(pcallStack.length - 1)
    } else {
      throw ThrowableVmError(ExtCallStackUnderflow)
    }

    callStack.remove(callStack.length - 1) match {
      case (Some(_), _) => setCounter(lastOffset)
      case _            => throw ThrowableVmError(ExtCallStackUnderflow)
    }
  }

  def makeRet(): Unit = {
    if (localCallStack.isEmpty) {
      throw ThrowableVmError(CallStackUnderflow)
    }
    setCounter(localCallStack.remove(localCallStack.length - 1))
  }

  def makeCall(newOffset: Int): Unit = {
    if (localCallStack.length >= 1024) {
      throw ThrowableVmError(CallStackOverflow)
    }
    localCallStack += currentCounter
    setCounter(newOffset)
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

  def heapPut(x: Data): Data.Primitive.Ref = {
    heap += x
    Data.Primitive.Ref(heap.length - 1)
  }

  def heapGet(ref: Data.Primitive.Ref): Data = {
    val idx = ref.data
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
