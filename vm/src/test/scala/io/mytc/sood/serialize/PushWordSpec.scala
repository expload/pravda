package io.mytc.sood
package serialize

import java.nio.ByteBuffer

import io.mytc.sood.vm.Vm
import io.mytc.sood.vm.Vm.PUSHX
import org.scalatest.{FlatSpec, Matchers}

class PushWordSpec extends FlatSpec with Matchers {


  "One-byte word" should "be converted to 1 Byte and 6 bits" in {
    val program = bytes(PUSHX, LEN1 | 0x3F, 0xFF, 0x00)
    Vm.run(ByteBuffer.wrap(program), None).toArray shouldBe Array(bytes(0x3F, 0xFF))
  }

  "Three-byte word" should "be converted to 3 Bytes and 6 bits" in {
    val program = bytes(PUSHX, LEN3 | 0x2F, 0xCA, 0xAB, 0xFE, 0x00)
    Vm.run(ByteBuffer.wrap(program), None).toArray shouldBe Array(
      bytes(0x2F, 0xCA, 0xAB, 0xFE)
    )
  }

  "Four-byte word" should "be converted to 4 Bytes and 6 bits" in {
    val program = bytes(PUSHX, LEN4 | 0x2F, 0xCA, 0xAB, 0xFE, 0x00)
    Vm.run(ByteBuffer.wrap(program), None).toArray shouldBe Array(
      bytes(0x2F, 0xCA, 0xAB, 0xFE, 0x00)
    )
  }
}
