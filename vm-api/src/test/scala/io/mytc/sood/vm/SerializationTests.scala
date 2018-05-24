package pravda.vm

import java.nio.ByteBuffer

import utest._

import pravda.common.bytes.hex._

object SerializationTests extends TestSuite {

  def buf(i: Array[Byte]) = ByteBuffer.wrap(i)

  val LEN_OF_LEN_0 = 0x00.toByte
  val LEN_OF_LEN_1 = 0x20.toByte
  val LEN_OF_LEN_2 = 0x40.toByte
  val LEN_OF_LEN_3 = 0x60.toByte
  val LEN_OF_LEN_4 = 0x80.toByte
  val LEN_1 = 0x01.toByte
  val LEN_4 = 0x04.toByte

  val INTLEN = (LEN_OF_LEN_1 | LEN_4).toByte


  def tests = Tests {

    'wordToInt32Test - {
      assert(wordToInt32(buf(hex"$INTLEN 00 00 00 00")) == 0)
      assert(wordToInt32(buf(hex"$INTLEN 00 00 00 05")) == 5)
      assert(wordToInt32(buf(hex"$INTLEN 00 00 00 fa")) == 0xFA)
      assert(wordToInt32(buf(hex"$INTLEN 00 00 01 ff")) == 0x1FF)
      assert(wordToInt32(buf(hex"$INTLEN 00 00 01 00")) == 256)
      assert(wordToInt32(buf(hex"$INTLEN 00 01 00 00")) == 65536)
      assert(wordToInt32(buf(hex"$INTLEN 01 00 00 00")) == 16777216)
      assert(wordToInt32(buf(hex"$INTLEN 00 00 ff ff")) == 0xFFFF)
      assert(wordToInt32(buf(hex"$INTLEN 00 ff ff ff")) == 0xFFFFFF)
      assert(wordToInt32(buf(hex"$INTLEN 00 ff ff ff")) == 16777215)
      assert(wordToInt32(buf(hex"$INTLEN ff ff ff ff")) == 0xFFFFFFFF)
      assert(wordToInt32(buf(hex"$INTLEN ff ff ff ff")) == -1)
    }

    'int32ToWordTest - {
      int32ToWord(0) ==> hex"$INTLEN 00 00 00 00"
      int32ToWord(1) ==> hex"$INTLEN 00 00 00 01"
      int32ToWord(256) ==> hex"$INTLEN 00 00 01 00"
      int32ToWord(65536) ==> hex"$INTLEN 00 01 00 00"
      int32ToWord(16777216) ==> hex"$INTLEN 01 00 00 00"
      int32ToWord(16777215) ==> hex"$INTLEN 00 ff ff ff"
      int32ToWord(-1) ==> hex"$INTLEN ff ff ff ff"
    }

    'lessThen5bitsSizeTest - {
      bytesToWord(hex"03") ==> hex"03"
      bytesToWord(hex"07") ==> hex"07"
      bytesToWord(hex"1f") ==> hex"1f"
    }

    'lessThen5bitsSizeTest2 - {
      wordToBytes(buf(hex"03")) ==> hex"03"
      wordToBytes(buf(hex"07")) ==> hex"07"
      wordToBytes(buf(hex"1f")) ==> hex"1f"
    }

    'moreThen5bitsSizeTest - {
      bytesToWord(hex"20") ==> hex"21 20"
      bytesToWord(hex"20") ==> hex"${(LEN_OF_LEN_1 | LEN_1).toByte} 20"
      bytesToWord(hex"01 02 03 04 05 06") ==> hex"${(LEN_OF_LEN_1 | 0x06).toByte} 01 02 03 04 05 06"

      val b32 = Array.fill[Byte](32)(1)
      bytesToWord(b32) ==> hex"$LEN_OF_LEN_2 20" ++ b32

      val b256 = Array.fill[Byte](256)(1)
      bytesToWord(b256) ==> hex"${(LEN_OF_LEN_2 | 1).toByte} 00" ++ b256

    }

    'moreThen5bitsSizeTest - {
      wordToBytes(buf(hex"21 20")) ==> hex"20"
      wordToBytes(buf(hex"${(LEN_OF_LEN_1 | LEN_1).toByte} 20")) ==> hex"20"
      wordToBytes(buf(hex"${(LEN_OF_LEN_1 | 0x06).toByte} 01 02 03 04 05 06")) ==> hex"01 02 03 04 05 06"

      val b32 = Array.fill[Byte](32)(1)
      wordToBytes(buf(hex"$LEN_OF_LEN_2 20" ++ b32)) ==> b32

      val b256 = Array.fill[Byte](256)(1)
      wordToBytes(buf(hex"${(LEN_OF_LEN_2 | 1).toByte} 00" ++ b256)) ==> b256

    }

  }
}
