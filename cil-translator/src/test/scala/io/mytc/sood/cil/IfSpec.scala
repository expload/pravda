package io.mytc.sood.cil

import fastparse.byte.all._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.TablesData._
import org.scalatest.{FlatSpec, Matchers}

class IfSpec extends FlatSpec with Matchers {
  "if" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = PeParsersUtils.parsePe("if.exe")

    cilData.tables shouldBe TablesData(
      List(FieldData(22, "x", hex"0x0608")),
      List(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      List(MethodDefData(0, 150, "Main", hex"0x000001", List()),
           MethodDefData(0, 6278, ".ctor", hex"0x200001", List()),
           MethodDefData(0, 6289, ".cctor", hex"0x000001", List())),
      List(),
      List(
        TypeDefData(0,
                    "<Module>",
                    "",
                    Ignored,
                    List(FieldData(22, "x", hex"0x0608")),
                    List(MethodDefData(0, 150, "Main", hex"0x000001", List()))),
        TypeDefData(1048577,
                    "Program",
                    "",
                    Ignored,
                    List(FieldData(22, "x", hex"0x0608")),
                    List(MethodDefData(0, 150, "Main", hex"0x000001", List())))
      )
    )

    opCodes shouldBe List(
      List(
        Nop,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI41,
        Clt,
        StLoc0,
        LdLoc0,
        BrFalseS(8),
        Nop,
        LdcI44,
        StSFld(FieldData(22, "x", hex"0x0608")),
        Nop,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI45,
        Cgt,
        StLoc1,
        LdLoc1,
        BrFalseS(22),
        Nop,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI46,
        Cgt,
        StLoc2,
        LdLoc2,
        BrFalseS(8),
        Nop,
        LdcI47,
        StSFld(FieldData(22, "x", hex"0x0608")),
        Nop,
        Nop,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI40,
        Cgt,
        StLoc3,
        LdLoc3,
        BrFalseS(10),
        Nop,
        LdcI44,
        StSFld(FieldData(22, "x", hex"0x0608")),
        Nop,
        BrS(8),
        Nop,
        LdcI45,
        StSFld(FieldData(22, "x", hex"0x0608")),
        Nop,
        Ret
      ),
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret),
      List(LdcI41, StSFld(FieldData(22, "x", hex"0x0608")), Ret)
    )
  }
}
