package io.mytc.sood.cil

import fastparse.byte.all._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.TablesData._
import org.scalatest.{FlatSpec, Matchers}

class ClosureSpec extends FlatSpec with Matchers {
  "closures" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = PeParsersUtils.parsePe("closure.exe")

    cilData.tables shouldBe List(
      List(Ignored),
      List(Ignored, Ignored, Ignored, Ignored, Ignored, Ignored, Ignored),
      List(Ignored, Ignored, Ignored),
      List(FieldData(6, "e", hex"0x0608")),
      List(),
      List(Ignored),
      List(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(57, ".ctor", hex"0x200001"),
        MemberRefData(12, ".ctor", hex"0x2002011c18"),
        MemberRefData(12, "Invoke", hex"0x200113011300"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      List(Ignored, Ignored, Ignored, Ignored),
      List(Ignored),
      List(Ignored),
      List(Ignored),
      List(Ignored),
      List(Ignored)
    )

    opCodes shouldBe List(
      List(
        NewObj(???),
        StLoc0,
        Nop,
        LdLoc0,
        LdcI41,
        StFld(FieldData(6, "e", hex"0x0608")),
        LdLoc0,
        LdFtn(???),
        NewObj(MemberRefData(12, ".ctor", hex"0x2002011c18")),
        StLoc1,
        LdLoc1,
        LdcI43,
        CallVirt(MemberRefData(12, "Invoke", hex"0x200113011300")),
        StLoc2,
        Ret
      ),
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret),
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret),
      List(LdArg1, LdArg0, LdFld(FieldData(6, "e", hex"0x0608")), Add, Ret)
    )
  }
}
