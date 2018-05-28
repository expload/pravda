package io.mytc.sood.cil

//import fastparse.byte.all._
//import io.mytc.sood.cil.CIL._
//import io.mytc.sood.cil.TablesData._
import utest._
//import io.mytc.sood.vm.bytes

object LoopTests extends TestSuite {

  val tests = Tests {
    'loopParse - {

//      val Right((_, cilData, methods)) = PeParsersUtils.parsePe("loop.exe")
//
//      cilData.tables ==> TablesData(
//        List(),
//        List(
//          MemberRefData(9, ".ctor", hex"0x20010108"),
//          MemberRefData(17, ".ctor", hex"0x200001"),
//          MemberRefData(25, ".ctor", hex"0x2001011111"),
//          MemberRefData(41, ".ctor", hex"0x200001")
//        ),
//        List(MethodDefData(0, 150, "Main", hex"0x000001", List()),
//             MethodDefData(0, 6278, ".ctor", hex"0x200001", List())),
//        List(),
//        List(
//          TypeDefData(0, "<Module>", "", Ignored, List(), List(MethodDefData(0, 150, "Main", hex"0x000001", List()))),
//          TypeDefData(1048577,
//                      "Program",
//                      "",
//                      Ignored,
//                      List(),
//                      List(MethodDefData(0, 150, "Main", hex"0x000001", List())))
//        ),
//        List(
//          StandAloneSigData(LocalVarSig(
//            Seq(LocalVar(I4, false), LocalVar(I4, false), LocalVar(Boolean, false), LocalVar(Boolean, false)))))
//      )
//
//      methods ==> List(
//        Method(
//          List(
//            Nop,
//            LdcI40,
//            StLoc0,
//            LdcI40,
//            StLoc1,
//            BrS(10),
//            Nop,
//            LdLoc0,
//            LdcI42,
//            Add,
//            StLoc0,
//            Nop,
//            LdLoc1,
//            LdcI41,
//            Add,
//            StLoc1,
//            LdLoc1,
//            LdcI4S(10),
//            Clt,
//            StLoc2,
//            LdLoc2,
//            BrTrueS(-19),
//            BrS(6),
//            Nop,
//            LdLoc0,
//            LdcI42,
//            Mull,
//            StLoc0,
//            Nop,
//            LdLoc0,
//            LdcI4(10000),
//            Clt,
//            StLoc3,
//            LdLoc3,
//            BrTrueS(-18),
//            Ret
//          ),
//          2,
//          LocalVarSig(Seq(LocalVar(I4, false), LocalVar(I4, false), LocalVar(Boolean, false), LocalVar(Boolean, false)))
//        ),
//        Method(List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret), 0, LocalVarSig(Seq.empty))
//      )
//
//      import io.mytc.sood.asm.Datum._
//      import io.mytc.sood.asm.Op._
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
//        Jump("stop"),
//        Label("methodMain"),
//        Push(Integral(0)),
//        Push(Integral(0)),
//        Push(Integral(0)),
//        Push(Integral(0)),
//        Nop,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 0))),
//        Push(Integral(5)),
//        SwapN,
//        Pop,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 0))),
//        Push(Integral(4)),
//        SwapN,
//        Pop,
//        Jump("br17"),
//        Label("br7"),
//        Nop,
//        Push(Integral(4)),
//        Dupn,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 2))),
//        LCall("Typed", "typedAdd", 2),
//        Push(Integral(5)),
//        SwapN,
//        Pop,
//        Nop,
//        Push(Integral(3)),
//        Dupn,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 1))),
//        LCall("Typed", "typedAdd", 2),
//        Push(Integral(4)),
//        SwapN,
//        Pop,
//        Label("br17"),
//        Push(Integral(3)),
//        Dupn,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 10))),
//        LCall("Typed", "typedClt", 2),
//        Push(Integral(3)),
//        SwapN,
//        Pop,
//        Push(Integral(2)),
//        Dupn,
//        JumpI("br7"),
//        Jump("br34"),
//        Label("br28"),
//        Nop,
//        Push(Integral(4)),
//        Dupn,
//        Push(Rawbytes(bytes(1, 0, 0, 0, 2))),
//        LCall("Typed", "typedMull", 2),
//        Push(Integral(5)),
//        SwapN,
//        Pop,
//        Nop,
//        Label("br34"),
//        Push(Integral(4)),
//        Dupn,
//        Push(Rawbytes(bytes(1, 0, 0, 39, 16))),
//        LCall("Typed", "typedClt", 2),
//        Push(Integral(2)),
//        SwapN,
//        Pop,
//        Push(Integral(1)),
//        Dupn,
//        JumpI("br28"),
//        Jump("stop"),
//        Label("method.ctor"),
//        Push(Integral(1)),
//        Dupn,
//        Nop,
//        Jump("stop"),
//        Label("stop")
//      )
    }
  }
}
