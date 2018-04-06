package io.mytc.sood

import java.nio.ByteBuffer

import scala.annotation.tailrec

package object vm {

  def int32ToWord(int32: Int): Array[Byte] = {
    val w = new Array[Byte](4)
    w(0) = (int32 >> 24).toByte
    w(1) = (int32 >> 16 & 0xFF).toByte
    w(2) = (int32 >> 8 & 0xFF).toByte
    w(3) = (int32 & 0xFF).toByte
    w
  }

  def wordToInt32(word: Array[Byte]): Int = {
    word(0) << 24 |
    (word(1) & 0xFF) << 16 |
    (word(2) & 0xFF) << 8 |
    (word(3) & 0xFF)
  }

  def bytes(b: Int*): Array[Byte] = {
    b.map(_.toByte).toArray
  }

  def readWord(source: ByteBuffer, len: Int): Array[Byte] = {
    val word = new Array[Byte](len)
    @tailrec def aux(i: Int): Unit = {
      if (i <= len) {
        word(i) = source.get()
        aux(i + 1)
      }
    }
    aux(0)
    word
  }

}
