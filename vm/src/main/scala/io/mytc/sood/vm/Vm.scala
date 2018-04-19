package io.mytc.sood.vm

import java.nio.ByteBuffer

import scala.annotation.switch
import scala.collection.mutable.ArrayBuffer

object Vm {

  import Opcodes.int._

  type Data = Array[Byte]

  def run(programm: ByteBuffer, enclosingStack: Option[ArrayBuffer[Data]]): Seq[Data] = {
    val callStack = new ArrayBuffer[Int](1024)
    val stack = enclosingStack.getOrElse(new ArrayBuffer[Data](1024))
    val heap = new ArrayBuffer[Data](1024)

    def callPop(): Int = {
      callStack.remove(callStack.length - 1)
    }

    def callPush(pos: Int): Unit = {
      callStack += pos
    }

    def pop() =
      stack.remove(stack.length - 1)

    def push(x: Data) =
      stack += x

    def aux(): Unit = if (programm.hasRemaining) {
      (programm.get() & 0xff: @switch) match {
        case CALL =>
          callPush(programm.position())
          programm.position(dataToInt32(pop()))
          aux()
        case RET =>
          programm.position(callPop())
          aux()
        case JUMP =>
          programm.position(dataToInt32(pop()))
          aux()
        case JUMPI =>
          if (pop().sum > 0)
            programm.position(dataToInt32(pop()))
          else pop()
          aux()
        case PUSHX =>
          push(dataFromWord(programm))
          aux()
        case POP =>
          pop()
          aux()
        case DUP =>
          val x = pop()
          push(x)
          push(x)
          aux()
        case SWAP =>
          val fsti = stack.length - 1
          val sndi = fsti - 1
          val fst = stack(fsti)
          val snd = stack(sndi)
          stack(fsti) = snd
          stack(sndi) = fst
          aux()
        case MPUT =>
          val i = heap.length
          heap += pop()
          push(int32ToData(i))
          aux()
        case MGET =>
          val i = dataToInt32(pop())
          push(heap(i))
          aux()
        case I32ADD =>
          push(int32ToData(dataToInt32(pop()) + dataToInt32(pop())))
          aux()
        case I32MUL =>
          push(int32ToData(dataToInt32(pop()) * dataToInt32(pop())))
          aux()
        case I32DIV =>
          push(int32ToData(dataToInt32(pop()) / dataToInt32(pop())))
          aux()
        case I32MOD =>
          push(int32ToData(dataToInt32(pop()) % dataToInt32(pop())))
          aux()
        case STOP => ()
      }
    }
    programm.rewind()
    aux()
    // Return stack
    stack
  }

  def dataToInt32(data: Data): Int = {
    ByteBuffer.wrap(data).getInt
  }

  def int32ToData(i: Int): Data = {
    val buf = ByteBuffer.allocate(4)
    buf.putInt(i)
    buf.array()
  }

  def dataFromWord(source: ByteBuffer): Data = {
    wordToBytes(source)
  }

}
