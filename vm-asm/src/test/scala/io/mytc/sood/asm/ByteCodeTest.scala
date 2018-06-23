package pravda.vm.asm

import utest._


object ByteCodeTest extends TestSuite {

  import pravda.vm
  import java.nio.ByteBuffer

  def raw(v: Int): Datum = {
    Datum.Rawbytes(vm.wordToBytes(ByteBuffer.wrap(vm.int32ToWord(v))))
  }

  def roundTrip(opcodes: Seq[Op]): Unit = {
    val bc = ByteCode()
    val bytes = bc.gen(opcodes)
    val opcodes2 = bc.ungen(bytes)

    opcodes2 ==> opcodes2
  }

  def tests = Tests {
    "A bc must correctly disassemble binary" - {
      val bc = ByteCode()
      val prog = Array(0x11, 0x24, 0x00, 0x00, 0x00, 0x03, 0x11, 0x24, 0x00, 0x00, 0x00, 0x05, 0x60).map(_.toByte)
      val unit = bc.ungen(prog)
      assert(unit == Seq(
        (0, Op.Push(raw(3))),
        (6, Op.Push(raw(5))),
        (12, Op.Add)
      ))
    }

    "opcodes must be converted to byte-code and vice versa correctly" - {

      "lcall" - {
        roundTrip(Seq(Op.LCall("Typed", "typedAdd", 2)))
      }

      "pcall" - {
        roundTrip(Seq(Op.PCall))
      }

      "sexist" - {
        roundTrip(Seq(Op.SExst))
      }

      "swapn" - {
        roundTrip(Seq(Op.SwapN))
      }
    }
  }

}
