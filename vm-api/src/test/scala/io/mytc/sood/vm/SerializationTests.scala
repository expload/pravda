package io.mytc.sood.vm

import java.nio.ByteBuffer

import utest._

object SerializationTests extends TestSuite {

  def buf(i: Int*) = ByteBuffer.wrap(bytes(i:_*))
  def buf(i: Array[Byte]) = ByteBuffer.wrap(i)

  val LEN_OF_LEN_0 = 0x00
  val LEN_OF_LEN_1 = 0x20
  val LEN_OF_LEN_2 = 0x40
  val LEN_OF_LEN_3 = 0x60
  val LEN_OF_LEN_4 = 0x80
  val LEN_1 = 0x01
  val LEN_4 = 0x04

  val INTLEN = LEN_OF_LEN_1 | LEN_4


  def tests = Tests {

    'wordToInt32Test - {
      assert(wordToInt32(buf(INTLEN, 0, 0, 0, 0)) == 0)
      assert(wordToInt32(buf(INTLEN, 0, 0, 0, 5)) == 5)
      assert(wordToInt32(buf(INTLEN, 0, 0, 0, 0xFA)) == 0xFA)
      assert(wordToInt32(buf(INTLEN, 0, 0, 1, 0xFF)) == 0x1FF)
      assert(wordToInt32(buf(INTLEN, 0, 0, 1, 0)) == 256)
      assert(wordToInt32(buf(INTLEN, 0, 1, 0, 0)) == 65536)
      assert(wordToInt32(buf(INTLEN, 1, 0, 0, 0)) == 16777216)
      assert(wordToInt32(buf(INTLEN, 0, 0, 0xFF, 0xFF)) == 0xFFFF)
      assert(wordToInt32(buf(INTLEN, 0, 0xFF, 0xFF, 0xFF)) == 0xFFFFFF)
      assert(wordToInt32(buf(INTLEN, 0, 0xFF, 0xFF, 0xFF)) == 16777215)
      assert(wordToInt32(buf(INTLEN, 0xFF, 0xFF, 0xFF, 0xFF)) == 0xFFFFFFFF)
      assert(wordToInt32(buf(INTLEN, 0xFF, 0xFF, 0xFF, 0xFF)) == -1)
    }

    'int32ToWordTest - {
      int32ToWord(0) ==> bytes(INTLEN, 0, 0, 0, 0)
      int32ToWord(1) ==> bytes(INTLEN, 0, 0, 0, 1)
      int32ToWord(256) ==> bytes(INTLEN, 0, 0, 1, 0)
      int32ToWord(65536) ==> bytes(INTLEN, 0, 1, 0, 0)
      int32ToWord(16777216) ==> bytes(INTLEN, 1, 0, 0, 0)
      int32ToWord(16777215) ==> bytes(INTLEN, 0, 255, 255, 255)
      int32ToWord(-1) ==> bytes(INTLEN, 255, 255, 255, 255)
    }

    'lessThen5bitsSizeTest - {
      bytesToWord(bytes(3)) ==> bytes(3)
      bytesToWord(bytes(7)) ==> bytes(7)
      bytesToWord(bytes(31)) ==> bytes(31)
    }

    'lessThen5bitsSizeTest2 - {
      wordToBytes(buf(3)) ==> bytes(3)
      wordToBytes(buf(7)) ==> bytes(7)
      wordToBytes(buf(31)) ==> bytes(31)
    }

    'moreThen5bitsSizeTest - {
      bytesToWord(bytes(32)) ==> bytes(0x21, 32)
      bytesToWord(bytes(32)) ==> bytes(LEN_OF_LEN_1 | LEN_1, 32)
      bytesToWord(bytes(1, 2, 3, 4, 5, 6)) ==> bytes(LEN_OF_LEN_1 | 0x06, 1, 2, 3, 4, 5, 6)

      val b32 = Array.fill[Byte](32)(1)
      bytesToWord(b32) ==> bytes(LEN_OF_LEN_2, 32) ++ b32

      val b256 = Array.fill[Byte](256)(1)
      bytesToWord(b256) ==> bytes(LEN_OF_LEN_2 | 1, 0) ++ b256

    }

    'moreThen5bitsSizeTest - {
      wordToBytes(buf(0x21, 32)) ==> bytes(32)
      wordToBytes(buf(LEN_OF_LEN_1 | LEN_1, 32)) ==> bytes(32)
      wordToBytes(buf(LEN_OF_LEN_1 | 0x06, 1, 2, 3, 4, 5, 6)) ==> bytes(1, 2, 3, 4, 5, 6)

      val b32 = Array.fill[Byte](32)(1)
      wordToBytes(buf(bytes(LEN_OF_LEN_2, 32) ++ b32)) ==> b32

      val b256 = Array.fill[Byte](256)(1)
      wordToBytes(buf(bytes(LEN_OF_LEN_2 | 1, 0) ++ b256)) ==> b256

    }

  }
}
