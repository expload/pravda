package io.mytc.sood

import java.nio.ByteBuffer

import scala.annotation.{switch, tailrec}
import scala.collection.mutable.ArrayBuffer

object Vm {

  type Word = Array[Byte]

  // Control
  final val STOP = 0x00.toByte
  final val JUMP = 0x01.toByte
  final val JUMPI = 0x02.toByte
  //final val RUN = 0x03.toByte

  // Stack
  final val POP = 0x10.toByte
  final val PUSHX = 0x11.toByte
  final val DUP = 0x12.toByte
  final val SWAP = 0x23.toByte

  // Heap
  final val MPUT = 0x40.toByte
  final val MGET = 0x41.toByte

  // Int32 operations
  final val I32ADD = 0x60.toByte
  final val I32MUL = 0x61.toByte
  final val I32DIV = 0x62.toByte
  final val I32MOD = 0x63.toByte

  def run(programm: ByteBuffer, enclosingStack: Option[ArrayBuffer[Word]]): Seq[Word] = {
    val stack = enclosingStack.getOrElse(new ArrayBuffer[Word](1024))
    val heap = new ArrayBuffer[Word](1024)

    def pop() =
      stack.remove(stack.length - 1)

    def push(x: Word) =
      stack += x

    def aux(): Unit = if (programm.hasRemaining) {
      (programm.get(): @switch) match {
        case JUMP =>
          programm.position(wordToInt32(pop()))
          aux()
        case JUMPI =>
          if (pop().sum > 0)
            programm.position(wordToInt32(pop()))
          aux()
        case PUSHX =>
          push(readWord(programm))
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

  def readWord(source: ByteBuffer): Word = {
    val firstByte = source.get()
    val len = (firstByte & 0xC0) >> 6
    val word = new Array[Byte](len)
    if (len > 0) {
      @tailrec def aux(i: Int): Unit = {
        if (i <= len) {
          word(i) = source.get()
          aux(i + 1)
        }
      }
      word(0) = firstByte
      aux(1)
    }
    word
  }

  def int32ToWord(int32: Int): Word = {
    val w = new Array[Byte](4)
    w(0) = (int32 >> 24).toByte
    w(1) = (int32 >> 16 & 0xFF).toByte
    w(2) = (int32 >> 8 & 0xFF).toByte
    w(3) = (int32 & 0xFF).toByte
    w
  }

  def wordToInt32(word: Word): Int =
    word(0) << 24 |
    word(1) << 16 |
    word(2) << 8  |
    word(3) & 0x000000FF
}

