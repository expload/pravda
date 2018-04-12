package io.mytc.sood.asm

import org.scalatest._


class ByteCodeTest extends FlatSpec with Matchers {

  "A bc" must "correctly disassemble binary" in {
    val bc = ByteCode()
    val prog = Array(0x11, 0x24, 0x00, 0x00, 0x00, 0x03, 0x11, 0x24, 0x00, 0x00, 0x00, 0x05, 0x60).map(_.toByte)
    val unit = bc.ungen(prog)
    assert(unit == Seq(Op.Push(3), Op.Push(5), Op.I32Add))
  }

}
