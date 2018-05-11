package io.mytc.sood.cil

import fastparse.byte.all._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.TablesData._
import org.scalatest.{FlatSpec, Matchers}

class ArithmeticsSpec extends FlatSpec with Matchers {
  "arithmetic operations" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = PeParsersUtils.parsePe("arithmetics.exe")

    cilData.tables shouldBe TablesData(
      List(FieldData(22, "x", hex"0x0608")),
      List(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      List(
        MethodDefData(0, 150, "Main", hex"0x000001", List()),
        MethodDefData(0, 6278, ".ctor", hex"0x200001", List()),
        MethodDefData(0, 6289, ".cctor", hex"0x000001", List())
      ),
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
        LdcI42,
        Add,
        StLoc0,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI42,
        Mull,
        StLoc1,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI42,
        Div,
        StLoc2,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI42,
        Rem,
        StLoc3,
        LdLoc0,
        LdLoc1,
        Add,
        LdcI4S(42),
        Add,
        LdLoc2,
        Mull,
        LdLoc3,
        Add,
        LdcI4(1337),
        Div,
        StLocS(4),
        Ret
      ),
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret),
      List(LdcI4S(10), StSFld(FieldData(22, "x", hex"0x0608")), Ret)
    )

    import io.mytc.sood.asm.Datum._
    import io.mytc.sood.asm.Op._

    opCodes.map(os => Translator.translate(Translator.CilContext(os))) shouldBe
      List(
        List(
          Push(Rawbytes("x".getBytes)),
          LCall("Classes", "loadField", 1),
          Push(Rawbytes(Array[Byte](1, 0, 0, 0, 2))),
          LCall("Typed", "typedAdd", 2),
          Push(Integral(0)),
          LCall("Local", "storeLocal", 2),
          Push(Rawbytes("x".getBytes)),
          LCall("Classes", "loadField", 1),
          Push(Rawbytes(Array[Byte](1, 0, 0, 0, 2))),
          LCall("Typed", "typedMull", 2),
          Push(Integral(1)),
          LCall("Local", "storeLocal", 2),
          Push(Rawbytes("x".getBytes)),
          LCall("Classes", "loadField", 1),
          Push(Rawbytes(Array[Byte](1, 0, 0, 0, 2))),
          LCall("Typed", "typedDiv", 2),
          Push(Integral(2)),
          LCall("Local", "storeLocal", 2),
          Push(Rawbytes("x".getBytes)),
          LCall("Classes", "loadField", 1),
          Push(Rawbytes(Array[Byte](1, 0, 0, 0, 2))),
          LCall("Typed", "typedMod", 2),
          Push(Integral(3)),
          LCall("Local", "storeLocal", 2),
          Push(Integral(0)),
          LCall("Local", "loadLocal", 1),
          Push(Integral(1)),
          LCall("Local", "loadLocal", 1),
          LCall("Typed", "typedAdd", 2),
          Push(Rawbytes(Array[Byte](1, 0, 0, 0, 42))),
          LCall("Typed", "typedAdd", 2),
          Push(Integral(2)),
          LCall("Local", "loadLocal", 1),
          LCall("Typed", "typedMull", 2),
          Push(Integral(3)),
          LCall("Local", "loadLocal", 1),
          LCall("Typed", "typedAdd", 2),
          LCall("Typed", "typedDiv", 2),
          Push(Integral(4)),
          LCall("Local", "storeLocal", 2)
        ),
        List(),
        List(Push(Rawbytes(Array[Byte](1, 0, 0, 0, 10))))
      )
  }
}
