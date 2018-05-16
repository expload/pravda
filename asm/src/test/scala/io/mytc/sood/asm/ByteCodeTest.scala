package io.mytc.sood.asm

import utest._


object ByteCodeTest extends TestSuite {

  import io.mytc.sood.vm
  import java.nio.ByteBuffer

  def raw(v: Int): Datum = {
    Datum.Rawbytes(vm.wordToBytes(ByteBuffer.wrap(vm.int32ToWord(v))))
  }

  def tests = Tests {
    "A bc must correctly disassemble binary" - {
      val bc = ByteCode()
      val prog = Array(0x11, 0x24, 0x00, 0x00, 0x00, 0x03, 0x11, 0x24, 0x00, 0x00, 0x00, 0x05, 0x60).map(_.toByte)
      val unit = bc.ungen(prog)
      assert(unit == Seq(
        (0, Op.Push(raw(3))),
        (6, Op.Push(raw(5))),
        (12, Op.I32Add)
      ))
    }

    "lcall must be converted to byte-code and vice versa correctly" - {
      val opcodes = Seq(Op.LCall("Typed", "typedAdd", 2))
      val bc = ByteCode()
      val bytes = bc.gen(opcodes)
      val opcodes2 = bc.ungen(bytes)

      opcodes2 ==> opcodes2
    }
  }

}
