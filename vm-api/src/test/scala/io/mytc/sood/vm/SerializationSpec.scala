package io.mytc.sood.vm

import java.nio.ByteBuffer

import org.scalatest.{FlatSpec, Matchers}

class SerializationSpec extends FlatSpec with Matchers {

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

  "WordToInt32 function" should "deserialize integers" in {
    wordToInt32(buf(INTLEN, 0, 0, 0, 0)) shouldBe 0
    wordToInt32(buf(INTLEN, 0, 0, 0, 5)) shouldBe 5
    wordToInt32(buf(INTLEN, 0, 0, 0, 0xFA)) shouldBe 0xFA
    wordToInt32(buf(INTLEN, 0, 0, 1, 0xFF)) shouldBe 0x1FF
    wordToInt32(buf(INTLEN, 0, 0, 1, 0)) shouldBe 256
    wordToInt32(buf(INTLEN, 0, 1, 0, 0)) shouldBe 65536
    wordToInt32(buf(INTLEN, 1, 0, 0, 0)) shouldBe 16777216
    wordToInt32(buf(INTLEN, 0, 0, 0xFF, 0xFF)) shouldBe 0xFFFF
    wordToInt32(buf(INTLEN, 0, 0xFF, 0xFF, 0xFF)) shouldBe 0xFFFFFF
    wordToInt32(buf(INTLEN, 0, 0xFF, 0xFF, 0xFF)) shouldBe 16777215
    wordToInt32(buf(INTLEN, 0xFF, 0xFF, 0xFF, 0xFF)) shouldBe 0xFFFFFFFF
    wordToInt32(buf(INTLEN, 0xFF, 0xFF, 0xFF, 0xFF)) shouldBe -1
  }

  "Int32ToWord function" should "serialize integers" in {
    int32ToWord(0) shouldBe bytes(INTLEN, 0, 0, 0, 0)
    int32ToWord(1) shouldBe bytes(INTLEN, 0, 0, 0, 1)
    int32ToWord(256) shouldBe bytes(INTLEN, 0, 0, 1, 0)
    int32ToWord(65536) shouldBe bytes(INTLEN, 0, 1, 0, 0)
    int32ToWord(16777216) shouldBe bytes(INTLEN, 1, 0, 0, 0)
    int32ToWord(16777215) shouldBe bytes(INTLEN, 0, 255, 255, 255)
    int32ToWord(-1) shouldBe bytes(INTLEN, 255, 255, 255, 255)
  }

  "Words with size less than 5 bits" should "be represented in 1 byte" in {
    bytesToWord(bytes(3)) shouldBe bytes(3)
    bytesToWord(bytes(7)) shouldBe bytes(7)
    bytesToWord(bytes(31)) shouldBe bytes(31)
    bytesToWord(bytes(32)) should not be bytes(32)
  }

  "Words with size less than 5 bits" should "be recognized as byte of data" in {
    wordToBytes(buf(3)) shouldBe bytes(3)
    wordToBytes(buf(7)) shouldBe bytes(7)
    wordToBytes(buf(31)) shouldBe bytes(31)
  }

  "Words with size more than 5 bits" should "be represented according to the specification" in {
    bytesToWord(bytes(32)) shouldBe bytes(0x21, 32)
    bytesToWord(bytes(32)) shouldBe bytes(LEN_OF_LEN_1 | LEN_1, 32)
    bytesToWord(bytes(1, 2, 3, 4, 5, 6)) shouldBe bytes(LEN_OF_LEN_1 | 0x06, 1, 2, 3, 4, 5, 6)

    val b32 = Array.fill[Byte](32)(1)
    bytesToWord(b32) shouldBe bytes(LEN_OF_LEN_2, 32) ++ b32

    val b256 = Array.fill[Byte](256)(1)
    bytesToWord(b256) shouldBe bytes(LEN_OF_LEN_2 | 1, 0) ++ b256

  }

  "Words with size more than 5 bits" should "be recognized according to the specification" in {
    wordToBytes(buf(0x21, 32)) shouldBe bytes(32)
    wordToBytes(buf(LEN_OF_LEN_1 | LEN_1, 32)) shouldBe bytes(32)
    wordToBytes(buf(LEN_OF_LEN_1 | 0x06, 1, 2, 3, 4, 5, 6)) shouldBe bytes(1, 2, 3, 4, 5, 6)

    val b32 = Array.fill[Byte](32)(1)
    wordToBytes(buf(bytes(LEN_OF_LEN_2, 32) ++ b32)) shouldBe b32

    val b256 = Array.fill[Byte](256)(1)
    wordToBytes(buf(bytes(LEN_OF_LEN_2 | 1, 0) ++ b256)) shouldBe b256

  }


}
