package io.mytc.sood.vm

import java.nio.ByteBuffer

import scala.annotation.{switch, tailrec}
import scala.collection.mutable.ArrayBuffer

object Vm {

  type Word = Array[Byte]

  import Opcodes.int._

  def run(programm: ByteBuffer, enclosingStack: Option[ArrayBuffer[Word]]): Seq[Word] = {
    val callStack = new ArrayBuffer[Int](1024)
    val stack = enclosingStack.getOrElse(new ArrayBuffer[Word](1024))
    val heap = new ArrayBuffer[Word](1024)

    def callPop(): Int = {
      callStack.remove(callStack.length - 1)
    }

    def callPush(pos: Int): Unit = {
      callStack += pos
    }

    def pop() =
      stack.remove(stack.length - 1)

    def push(x: Word) =
      stack += x

    def aux(): Unit = if (programm.hasRemaining) {
      (programm.get() & 0xff: @switch) match {
        case CALL =>
          callPush(programm.position())
          programm.position(wordToInt32(pop()))
          aux()
        case RET =>
          programm.position(callPop())
          aux()
        case JUMP =>
          programm.position(wordToInt32(pop()))
          aux()
        case JUMPI =>
          if (pop().sum > 0)
            programm.position(wordToInt32(pop()))
          else pop()
          aux()
        case PUSHX =>
          push(readXWord(programm))
          aux()
        case PUSH1 =>
          push(readWord(programm, 1))
          aux()
        case PUSH2 =>
          push(readWord(programm, 2))
          aux()
        case PUSH4 =>
          push(readWord(programm, 4))
          aux()
        case PUSH8 =>
          push(readWord(programm, 8))
          aux()
        case PUSH32 =>
          push(readWord(programm, 32))
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
          push(int32ToWord(i))
          aux()
        case MGET =>
          val i = wordToInt32(pop())
          push(heap(i))
          aux()
        case I32ADD =>
          push(int32ToWord(wordToInt32(pop()) + wordToInt32(pop())))
          aux()
        case I32MUL =>
          push(int32ToWord(wordToInt32(pop()) * wordToInt32(pop())))
          aux()
        case I32DIV =>
          push(int32ToWord(wordToInt32(pop()) / wordToInt32(pop())))
          aux()
        case I32MOD =>
          push(int32ToWord(wordToInt32(pop()) % wordToInt32(pop())))
          aux()
        case STOP => ()
      }
    }
    programm.rewind()
    aux()
    // Return stack
    stack
  }

  def readXWord(source: ByteBuffer): Word = {
    val firstByte = source.get()
    val len = ((firstByte & 0xC0) >> 6) + 1
    val word = new Array[Byte](len + 1)
    @tailrec def aux(i: Int): Unit = {
      if (i <= len) {
        word(i) = source.get()
        aux(i + 1)
      }
    }
    word(0) = (firstByte & 0x3F).toByte
    aux(1)
    word
  }

}
