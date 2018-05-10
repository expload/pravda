package io.mytc.sood.cil

import fastparse.byte.all._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.TablesData._
import org.scalatest.{FlatSpec, Matchers}

class ObjectsSpec extends FlatSpec with Matchers {
  "objects" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = PeParsersUtils.parsePe("objects.exe")

    cilData.tables shouldBe List(
      List(Ignored),
      List(Ignored, Ignored, Ignored, Ignored, Ignored),
      List(Ignored, Ignored, Ignored, Ignored),
      List(FieldData(1, "a", hex"0x0608"), FieldData(1, "b", hex"0x0608")),
      List(),
      List(Ignored, Ignored),
      List(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      List(Ignored, Ignored, Ignored),
      List(Ignored, Ignored),
      List(Ignored),
      List(Ignored)
    )

    opCodes shouldBe List(
      List(LdArg0,
        Call(MemberRefData(41, ".ctor", hex"0x200001")),
        Nop,
        Nop,
        LdArg0,
        LdArg1,
        StFld(FieldData(1, "a", hex"0x0608")),
        Ret),
      List(Nop, LdArg0, LdFld(FieldData(1, "a", hex"0x0608")), LdcI4S(42), Add, StLoc0, BrS(0), LdLoc0, Ret),
      List(LdArg0,
        Call(MemberRefData(41, ".ctor", hex"0x200001")),
        Nop,
        Nop,
        LdArg0,
        LdArg1,
        StFld(FieldData(1, "b", hex"0x0608")),
        Ret),
      List(Nop, LdArg0, LdFld(FieldData(1, "b", hex"0x0608")), LdcI4S(42), Add, StLoc0, BrS(0), LdLoc0, Ret),
      List(
        Nop,
        LdcI4S(-42),
        NewObj(???),
        StLoc0,
        LdcI40,
        NewObj(???),
        StLoc1,
        LdLoc0,
        CallVirt(???),
        LdLoc1,
        CallVirt(???),
        Add,
        StLoc2,
        Ret
      ),
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret)
    )
  }
}
