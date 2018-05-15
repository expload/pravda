package io.mytc.sood.vm
package serialize

import VmUtils.{binaryData, emptyState}
import Opcodes.int.PUSHX
import com.google.protobuf.ByteString
import org.scalatest.{FlatSpec, Matchers}

class PushWordSpec extends FlatSpec with Matchers {

  final val LEN4 = 0x04
  final val LEN3 = 0x03
  final val LEN2 = 0x02
  final val LEN1 = 0x01
  final val ZERO = 0x00


  "One-byte word" should "be converted to 1 Byte" in {
    val program = bytes(PUSHX, 0x15)
    Vm.runRaw(ByteString.copyFrom(program), ByteString.EMPTY, emptyState).stack.toArray shouldBe Array(binaryData(0x15))
  }

  "Three-byte word" should "be converted to 3 Bytes" in {
    val program = bytes(PUSHX, LEN3 | 0x20, 0xCA, 0xAB, 0xFE, 0x00)
    Vm.runRaw(ByteString.copyFrom(program), ByteString.EMPTY, emptyState).stack.toArray shouldBe Array(
      binaryData(0xCA, 0xAB, 0xFE)
    )
  }

  "Four-byte word" should "be converted to 4 Bytes and 6 bits" in {
    val program = bytes(PUSHX, LEN4 | 0x20, 0xCA, 0xAB, 0xFE, 0x00)
    Vm.runRaw(ByteString.copyFrom(program), ByteString.EMPTY, emptyState).stack.toArray shouldBe Array(
      binaryData(0xCA, 0xAB, 0xFE, 0x00)
    )
  }
}
