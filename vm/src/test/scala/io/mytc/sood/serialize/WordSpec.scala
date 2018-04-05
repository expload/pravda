package io.mytc.sood
package serialize

import org.scalatest.{FlatSpec, Matchers}
import vm.Vm
class WordSpec extends FlatSpec with Matchers {

  "WordToInt32 function" should "deserialize integers" in {
    Vm.wordToInt32(bytes(0, 0, 0, 0, 0)) shouldBe 0
    Vm.wordToInt32(bytes(0, 0, 0, 0, 5)) shouldBe 5
    Vm.wordToInt32(bytes(0, 0, 0, 0, 0xFA)) shouldBe 0xFA
    Vm.wordToInt32(bytes(0, 0, 0, 1, 0xFF)) shouldBe 0x1FF
    Vm.wordToInt32(bytes(0, 0, 0, 1, 0)) shouldBe 256
    Vm.wordToInt32(bytes(0, 0, 1, 0, 0)) shouldBe 65536
    Vm.wordToInt32(bytes(0, 1, 0, 0, 0)) shouldBe 16777216
    Vm.wordToInt32(bytes(0, 0, 0, 0xFF, 0xFF)) shouldBe 0xFFFF
    Vm.wordToInt32(bytes(0, 0, 0xFF, 0xFF, 0xFF)) shouldBe 0xFFFFFF
    Vm.wordToInt32(bytes(0, 0, 0xFF, 0xFF, 0xFF)) shouldBe 16777215
    Vm.wordToInt32(bytes(0, 0xFF, 0xFF, 0xFF, 0xFF)) shouldBe 0xFFFFFFFF
    Vm.wordToInt32(bytes(0, 0xFF, 0xFF, 0xFF, 0xFF)) shouldBe -1
  }

  "Int32ToWord function" should "serialize integers" in {
    Vm.int32ToWord(0) shouldBe bytes(0, 0, 0, 0, 0)
    Vm.int32ToWord(1) shouldBe bytes(0, 0, 0, 0, 1)
    Vm.int32ToWord(256) shouldBe bytes(0, 0, 0, 1, 0)
    Vm.int32ToWord(65536) shouldBe bytes(0, 0, 1, 0, 0)
    Vm.int32ToWord(16777216) shouldBe bytes(0, 1, 0, 0, 0)
    Vm.int32ToWord(16777215) shouldBe bytes(0, 0, 255, 255, 255)
    Vm.int32ToWord(-1) shouldBe bytes(0, 255, 255, 255, 255)
  }

  }
