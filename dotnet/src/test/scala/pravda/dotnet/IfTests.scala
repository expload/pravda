package pravda.dotnet

//import fastparse.byte.all._
//import io.mytc.sood.cil.CIL._
//import io.mytc.sood.cil.TablesData._
import utest._

object IfTests extends TestSuite {

  val tests = Tests {
    'ifParse - {
//      val Right((_, cilData, methods)) = PeParsersUtils.parsePe("if.exe")
//
//      cilData.tables ==> TablesData(
//        List(FieldData(22, "x", hex"0x0608")),
//        List(
//          MemberRefData(9, ".ctor", hex"0x20010108"),
//          MemberRefData(17, ".ctor", hex"0x200001"),
//          MemberRefData(25, ".ctor", hex"0x2001011111"),
//          MemberRefData(41, ".ctor", hex"0x200001")
//        ),
//        List(
//          MethodDefData(0, 150, "Main", hex"0x000001", List()),
//          MethodDefData(0, 6278, ".ctor", hex"0x200001", List()),
//          MethodDefData(0, 6289, ".cctor", hex"0x000001", List())
//        ),
//        List(),
//        List(
//          TypeDefData(0,
//                      "<Module>",
//                      "",
//                      Ignored,
//                      List(FieldData(22, "x", hex"0x0608")),
//                      List(MethodDefData(0, 150, "Main", hex"0x000001", List()))),
//          TypeDefData(1048577,
//                      "Program",
//                      "",
//                      Ignored,
//                      List(FieldData(22, "x", hex"0x0608")),
//                      List(MethodDefData(0, 150, "Main", hex"0x000001", List())))
//        ),
//        List(StandAloneSigData(LocalVarSig(
//          Seq(LocalVar(Boolean, false), LocalVar(Boolean, false), LocalVar(Boolean, false), LocalVar(Boolean, false)))))
//      )
//
//      methods ==> List(
//        Method(
//          List(
//            Nop,
//            LdSFld(FieldData(22, "x", hex"0x0608")),
//            LdcI41,
//            Clt,
//            StLoc0,
//            LdLoc0,
//            BrFalseS(8),
//            Nop,
//            LdcI44,
//            StSFld(FieldData(22, "x", hex"0x0608")),
//            Nop,
//            LdSFld(FieldData(22, "x", hex"0x0608")),
//            LdcI45,
//            Cgt,
//            StLoc1,
//            LdLoc1,
//            BrFalseS(22),
//            Nop,
//            LdSFld(FieldData(22, "x", hex"0x0608")),
//            LdcI46,
//            Cgt,
//            StLoc2,
//            LdLoc2,
//            BrFalseS(8),
//            Nop,
//            LdcI47,
//            StSFld(FieldData(22, "x", hex"0x0608")),
//            Nop,
//            Nop,
//            LdSFld(FieldData(22, "x", hex"0x0608")),
//            LdcI40,
//            Cgt,
//            StLoc3,
//            LdLoc3,
//            BrFalseS(10),
//            Nop,
//            LdcI44,
//            StSFld(FieldData(22, "x", hex"0x0608")),
//            Nop,
//            BrS(8),
//            Nop,
//            LdcI45,
//            StSFld(FieldData(22, "x", hex"0x0608")),
//            Nop,
//            Ret
//          ),
//          2,
//          LocalVarSig(
//            Seq(LocalVar(Boolean, false), LocalVar(Boolean, false), LocalVar(Boolean, false), LocalVar(Boolean, false)))
//        ),
//        Method(List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret), 0, LocalVarSig(Seq.empty)),
//        Method(List(LdcI41, StSFld(FieldData(22, "x", hex"0x0608")), Ret), 0, LocalVarSig(Seq.empty))
//      )
//
//      import io.mytc.sood.asm.Datum._
//      import io.mytc.sood.asm.Op._
//      import io.mytc.sood.vm.bytes
//
//      Translator.translate(methods, cilData) ==> List(
//        Dup,
//        Push(Rawbytes("Main".getBytes)),
//        Eq,
//        JumpI("methodMain"),
//        Dup,
//        Push(Rawbytes(".ctor".getBytes)),
//        Eq,
//        JumpI("method.ctor"),
//        Dup,
//        Push(Rawbytes(".cctor".getBytes)),
//        Eq,
//        JumpI("method.cctor"),
//        Jump("stop"),
//        Label("methodMain"),
//        Push(Integral(0)),
//        Push(Integral(0)),
//        Push(Integral(0)),
//        Push(Integral(0)),
//        Nop,
//        Push(Rawbytes("x".getBytes)),
//        SGet,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 1))),
//        LCall("Typed", "typedClt", 2),
//        Push(Integral(5)),
//        SwapN,
//        Pop,
//        Push(Integral(4)),
//        Dupn,
//        Not,
//        JumpI("br21"),
//        Nop,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 4))),
//        Push(Rawbytes("x".getBytes)),
//        SPut,
//        Nop,
//        Label("br21"),
//        Push(Rawbytes("x".getBytes)),
//        SGet,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 5))),
//        Swap,
//        LCall("Typed", "typedClt", 2),
//        Push(Integral(4)),
//        SwapN,
//        Pop,
//        Push(Integral(3)),
//        Dupn,
//        Not,
//        JumpI("br55"),
//        Nop,
//        Push(Rawbytes("x".getBytes)),
//        SGet,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 6))),
//        Swap,
//        LCall("Typed", "typedClt", 2),
//        Push(Integral(3)),
//        SwapN,
//        Pop,
//        Push(Integral(2)),
//        Dupn,
//        Not,
//        JumpI("br54"),
//        Nop,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 7))),
//        Push(Rawbytes("x".getBytes)),
//        SPut,
//        Nop,
//        Label("br54"),
//        Nop,
//        Label("br55"),
//        Push(Rawbytes("x".getBytes)),
//        SGet,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 0))),
//        Swap,
//        LCall("Typed", "typedClt", 2),
//        Push(Integral(2)),
//        SwapN,
//        Pop,
//        Push(Integral(1)),
//        Dupn,
//        Not,
//        JumpI("br77"),
//        Nop,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 4))),
//        Push(Rawbytes("x".getBytes)),
//        SPut,
//        Nop,
//        Jump("br85"),
//        Label("br77"),
//        Nop,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 5))),
//        Push(Rawbytes("x".getBytes)),
//        SPut,
//        Nop,
//        Label("br85"),
//        Jump("stop"),
//        Label("method.ctor"),
//        Push(Integral(1)),
//        Dupn,
//        Nop,
//        Jump("stop"),
//        Label("method.cctor"),
//        Push(Rawbytes(bytes(1, 0, 0, 0, 1))),
//        Push(Rawbytes("x".getBytes)),
//        SPut,
//        Jump("stop"),
//        Label("stop")
//      )
    }
  }
}
