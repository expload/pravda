package io.mytc.sood.cil

import fastparse.byte.all._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.Signatures.Info.SigType._
import io.mytc.sood.cil.Signatures.Info._
import io.mytc.sood.cil.TablesData._
import utest._
import io.mytc.sood.vm.bytes

object ArithmeticsTests extends TestSuite {

  val tests = Tests {
    'arithmepticParse - {

      val Right((_, cilData, methods)) = PeParsersUtils.parsePe("arithmetics.exe")

      cilData.tables ==> TablesData(
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
        ),
        List(
          StandAloneSigData(
            LocalVarSig(
              Seq(LocalVar(I4, false),
                  LocalVar(I4, false),
                  LocalVar(I4, false),
                  LocalVar(I4, false),
                  LocalVar(I4, false)))))
      )

      methods ==> List(
        Method(
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
          2,
          LocalVarSig(
            Seq(LocalVar(I4, false),
                LocalVar(I4, false),
                LocalVar(I4, false),
                LocalVar(I4, false),
                LocalVar(I4, false)))
        ),
        Method(List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret), 0, LocalVarSig(Seq.empty)),
        Method(List(LdcI4S(10), StSFld(FieldData(22, "x", hex"0x0608")), Ret), 0, LocalVarSig(Seq.empty))
      )

      import pravda.vm.asm.Datum._
      import pravda.vm.asm.Op._

      Translator.translate(methods, cilData) ==>
        List(
          Dup,
          Push(Rawbytes("Main".getBytes)),
          Eq,
          JumpI("methodMain"),
          Dup,
          Push(Rawbytes(".ctor".getBytes)),
          Eq,
          JumpI("method.ctor"),
          Dup,
          Push(Rawbytes(".cctor".getBytes)),
          Eq,
          JumpI("method.cctor"),
          Jump("stop"),
          Label("methodMain"),
          Push(Integral(0)),
          Push(Integral(0)),
          Push(Integral(0)),
          Push(Integral(0)),
          Push(Integral(0)),
          Nop,
          Push(Rawbytes("x".getBytes)),
          SGet,
          Push(Rawbytes(bytes(1, 0, 0, 0, 2))),
          LCall("Typed", "typedAdd", 2),
          Push(Integral(6)),
          SwapN,
          Pop,
          Push(Rawbytes("x".getBytes)),
          SGet,
          Push(Rawbytes(bytes(1, 0, 0, 0, 2))),
          LCall("Typed", "typedMull", 2),
          Push(Integral(5)),
          SwapN,
          Pop,
          Push(Rawbytes("x".getBytes)),
          SGet,
          Push(Rawbytes(bytes(1, 0, 0, 0, 2))),
          LCall("Typed", "typedDiv", 2),
          Push(Integral(4)),
          SwapN,
          Pop,
          Push(Rawbytes("x".getBytes)),
          SGet,
          Push(Rawbytes(bytes(1, 0, 0, 0, 2))),
          LCall("Typed", "typedMod", 2),
          Push(Integral(3)),
          SwapN,
          Pop,
          Push(Integral(5)),
          Dupn,
          Push(Integral(5)),
          Dupn,
          LCall("Typed", "typedAdd", 2),
          Push(Rawbytes(bytes(1, 0, 0, 0, 42))),
          LCall("Typed", "typedAdd", 2),
          Push(Integral(4)),
          Dupn,
          LCall("Typed", "typedMull", 2),
          Push(Integral(3)),
          Dupn,
          LCall("Typed", "typedAdd", 2),
          LCall("Typed", "typedDiv", 2),
          Push(Integral(1)),
          SwapN,
          Pop,
          Jump("stop"),
          Label("method.ctor"),
          Push(Integral(1)),
          Dupn,
          Nop,
          Jump("stop"),
          Label("method.cctor"),
          Push(Rawbytes(bytes(1, 0, 0, 0, 10))),
          Push(Rawbytes("x".getBytes)),
          SPut,
          Jump("stop"),
          Label("stop")
        )
    }
  }
}
