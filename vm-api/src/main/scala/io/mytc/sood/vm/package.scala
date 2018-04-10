package io.mytc.sood

import java.nio.ByteBuffer

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

package object vm {

  type Word = Array[Byte]

  def int32ToWord(int32: Int): Word = {
    val buf = ByteBuffer.allocate(5)
    buf.put(0x24.toByte)
    buf.putInt(int32)
    buf.array()
  }

  def wordToInt32(word: ByteBuffer): Int = {
    assert(word.get() == 0x24.toByte)

    word.getInt()
  }

  def bytes(b: Int*): Array[Byte] = {
    b.map(_.toByte).toArray
  }

  def lengthToBytes(len: Long): Array[Byte] = {
    assert(len >= 0)
    assert(((len >> 56) & 0xFF) == 0)

    @tailrec def toBytes(len: Long, arr: ArrayBuffer[Byte]): ArrayBuffer[Byte] = {
      if(len == 0) arr
      else toBytes(len >> 8, arr :+ (len & 0xFF).toByte)
    }
    toBytes(len, new ArrayBuffer[Byte](7)).reverse.toArray
  }

  def bytesToWord(bs: Array[Byte], reduction: Boolean = false): Word = {
    val reducedBytes = if(reduction) bs.dropWhile(_ == 0) else bs
    if((reducedBytes.length == 1) && ((reducedBytes(0) & 0xE0) == 0)) {
      reducedBytes
    } else {
      val prefix = lengthToBytes(reducedBytes.length.toLong)
      if ((prefix(0) & 0xE0) == 0) {
        val len = ((prefix.length & 0x07) << 5).toByte
        prefix(0) = (prefix(0) | len).toByte
        prefix ++ reducedBytes
      } else {
        val len = (((prefix.length + 1) & 0x07) << 5).toByte
        len +: (prefix ++ reducedBytes)
      }
    }
  }

  def bytesToLength(arr: Array[Byte]): Long = {
    assert(arr.length < 8)

    val longBytes = Array.fill[Byte](8 - arr.length)(0) ++ arr
    ByteBuffer.wrap(longBytes).getLong
  }

  def wordToBytes(source: ByteBuffer): Array[Byte] = {
    val fb = source.get()
    val lenOfLen = fb >>> 5
    if(lenOfLen == 0) Array(fb)
    else {
      val lenBytes = new Array[Byte](lenOfLen)
      lenBytes(0) = (fb & 0x1F).toByte
      source.get(lenBytes, 1, lenOfLen - 1)
      val len = bytesToLength(lenBytes)
      val result = new Array[Byte](len.toInt)
      source.get(result)
      result
    }
  }


  def bytesFromWord(source: ByteBuffer, len: Int): Array[Byte] = {
    val bs = wordToBytes(source)
    Array.fill[Byte](len - bs.length)(0) ++ bs
  }

}
