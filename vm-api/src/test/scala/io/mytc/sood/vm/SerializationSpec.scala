package io.mytc.sood.vm

import org.scalatest.{FlatSpec, Matchers}

class SerializationSpec extends FlatSpec with Matchers {

  "WordToInt32 function" should "deserialize integers" in {
    wordToInt32(bytes(0, 0, 0, 0)) shouldBe 0
    wordToInt32(bytes(0, 0, 0, 5)) shouldBe 5
    wordToInt32(bytes(0, 0, 0, 0xFA)) shouldBe 0xFA
    wordToInt32(bytes(0, 0, 1, 0xFF)) shouldBe 0x1FF
    wordToInt32(bytes(0, 0, 1, 0)) shouldBe 256
    wordToInt32(bytes(0, 1, 0, 0)) shouldBe 65536
    wordToInt32(bytes(1, 0, 0, 0)) shouldBe 16777216
    wordToInt32(bytes(0, 0, 0xFF, 0xFF)) shouldBe 0xFFFF
    wordToInt32(bytes(0, 0xFF, 0xFF, 0xFF)) shouldBe 0xFFFFFF
    wordToInt32(bytes(0, 0xFF, 0xFF, 0xFF)) shouldBe 16777215
    wordToInt32(bytes(0xFF, 0xFF, 0xFF, 0xFF)) shouldBe 0xFFFFFFFF
    wordToInt32(bytes(0xFF, 0xFF, 0xFF, 0xFF)) shouldBe -1
  }

  "Int32ToWord function" should "serialize integers" in {
    int32ToWord(0) shouldBe bytes(0, 0, 0, 0)
    int32ToWord(1) shouldBe bytes(0, 0, 0, 1)
    int32ToWord(256) shouldBe bytes(0, 0, 1, 0)
    int32ToWord(65536) shouldBe bytes(0, 1, 0, 0)
    int32ToWord(16777216) shouldBe bytes(1, 0, 0, 0)
    int32ToWord(16777215) shouldBe bytes(0, 255, 255, 255)
    int32ToWord(-1) shouldBe bytes(255, 255, 255, 255)
  }

}
