package io.mytc.sood.cil

import fastparse.byte.all._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.TablesData._
import org.scalatest.{FlatSpec, Matchers}

class LoopSpec extends FlatSpec with Matchers {
  "loop" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = PeParsersUtils.parsePe("loop.exe")

    cilData.tables shouldBe List(
      List(Ignored),
      List(Ignored, Ignored, Ignored, Ignored, Ignored),
      List(Ignored, Ignored),
      List(),
      List(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      List(Ignored, Ignored, Ignored),
      List(Ignored),
      List(Ignored),
      List(Ignored)
    )
    opCodes shouldBe List(
      List(
        Nop,
        LdcI40,
        StLoc0,
        LdcI40,
        StLoc1,
        BrS(10),
        Nop,
        LdLoc0,
        LdcI42,
        Add,
        StLoc0,
        Nop,
        LdLoc1,
        LdcI41,
        Add,
        StLoc1,
        LdLoc1,
        LdcI4S(10),
        Clt,
        StLoc2,
        LdLoc2,
        BrTrueS(-19),
        BrS(6),
        Nop,
        LdLoc0,
        LdcI42,
        Mull,
        StLoc0,
        Nop,
        LdLoc0,
        LdcI4(10000),
        Clt,
        StLoc3,
        LdLoc3,
        BrTrueS(-18),
        Ret
      ),
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret)
    )
  }
}
