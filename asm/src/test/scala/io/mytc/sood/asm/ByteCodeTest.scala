package io.mytc.sood.asm

import org.scalatest._


class ByteCodeTest extends FlatSpec with Matchers {

  import io.mytc.sood.vm
  import java.nio.ByteBuffer

  def roundTrip(name: String, opcodes: Seq[Op]): Unit = {
    name should "be converted to byte-code and vice versa correctly" in {
      val bc = ByteCode()
      val bytes = bc.gen(opcodes)
      val opcodes2 = bc.ungen(bytes)

      opcodes2 shouldBe opcodes2
    }
  }

  def raw(v: Int): Datum = {
    Datum.Rawbytes(vm.wordToBytes(ByteBuffer.wrap(vm.int32ToWord(v))))
  }

  "A bc" must "correctly disassemble binary" in {
    val bc = ByteCode()
    val prog = Array(0x11, 0x24, 0x00, 0x00, 0x00, 0x03, 0x11, 0x24, 0x00, 0x00, 0x00, 0x05, 0x60).map(_.toByte)
    val unit = bc.ungen(prog)
    assert(unit == Seq(
      (0, Op.Push(raw(3))),
      (6, Op.Push(raw(5))),
      (12, Op.I32Add)
    ))
  }

  roundTrip("LCALL", Seq(Op.LCall("Typed", "typedAdd", 2)))
}
