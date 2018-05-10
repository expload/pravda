package io.mytc.sood.cil

import fastparse.byte.all._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.TablesData._
import org.scalatest.{FlatSpec, Matchers}

class MethodCallingSpec extends FlatSpec with Matchers {
  "method calling" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = PeParsersUtils.parsePe("method_calling.exe")

    cilData.tables shouldBe List(
      List(Ignored),
      List(Ignored, Ignored, Ignored, Ignored, Ignored),
      List(Ignored, Ignored),
      List(
      ),
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
      List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret),
      List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret),
      List(Nop, LdArg0, LdArg1, Add, StLoc0, BrS(0), LdLoc0, Ret),
      List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret),
      List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret),
      List(
        Nop,
        Call(???),
        StLoc0,
        Call(???),
        StLoc1,
        LdLoc0,
        LdLoc1,
        Call(???),
        StLoc2,
        NewObj(???),
        StLoc3,
        LdLoc3,
        CallVirt(???),
        StLocS(4),
        LdLoc3,
        CallVirt(???),
        StLocS(5),
        Ret
      ),
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret)
    )
  }
}
