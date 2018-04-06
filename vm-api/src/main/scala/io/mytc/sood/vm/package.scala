package io.mytc.sood

import java.nio.ByteBuffer

package object vm {

  def int32ToWord(int32: Int): Array[Byte] = {
    val buf = ByteBuffer.allocate(4)
    buf.putInt(int32)
    buf.array()
  }

  def wordToInt32(word: Array[Byte]): Int = {
    ByteBuffer.wrap(word).getInt
  }

  def bytes(b: Int*): Array[Byte] = {
    b.map(_.toByte).toArray
  }

  def readWord(source: ByteBuffer, len: Int): Array[Byte] = {
    val word = new Array[Byte](len)
    source.get(word)
    word
  }

}
