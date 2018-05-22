package io.mytc.sood.cil

//import fastparse.byte.all._
//import io.mytc.sood.cil.CIL._
//import io.mytc.sood.cil.TablesData._

import utest._

object MethodCallingTests extends TestSuite {

  val tests = Tests {
    'methodCallingParse - {

      // FIXME objects aren't implemented yet
//      val Right((_, cilData, opCodes)) = PeParsersUtils.parsePe("method_calling.exe")
//
//      cilData.tables ==> TablesData(
//        List(),
//        List(
//          MemberRefData(9, ".ctor", hex"0x20010108"),
//          MemberRefData(17, ".ctor", hex"0x200001"),
//          MemberRefData(25, ".ctor", hex"0x2001011111"),
//          MemberRefData(41, ".ctor", hex"0x200001")
//        ),
//        List(
//          MethodDefData(0, 150, "answer", hex"0x000008", List(ParamData(0, 1, "a"), ParamData(0, 2, "b"))),
//          MethodDefData(0, 145, "secretAnswer", hex"0x000008", List(ParamData(0, 1, "a"), ParamData(0, 2, "b"))),
//          MethodDefData(0, 150, "sum", hex"0x0002080808", List(ParamData(0, 1, "a"), ParamData(0, 2, "b"))),
//          MethodDefData(0, 134, "personalAnswer", hex"0x200008", List()),
//          MethodDefData(0, 129, "personalSecretAnswer", hex"0x200008", List()),
//          MethodDefData(0, 150, "Main", hex"0x000001", List()),
//          MethodDefData(0, 6278, ".ctor", hex"0x200001", List())
//        ),
//        List(ParamData(0, 1, "a"), ParamData(0, 2, "b")),
//        List(
//          TypeDefData(
//            0,
//            "<Module>",
//            "",
//            Ignored,
//            List(),
//            List(MethodDefData(0, 150, "answer", hex"0x000008", List(ParamData(0, 1, "a"), ParamData(0, 2, "b"))))),
//          TypeDefData(
//            1048577,
//            "Program",
//            "",
//            Ignored,
//            List(),
//            List(MethodDefData(0, 150, "answer", hex"0x000008", List(ParamData(0, 1, "a"), ParamData(0, 2, "b")))))
//        )
//      )
//
//      opCodes ==> List(
//        List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret),
//        List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret),
//        List(Nop, LdArg0, LdArg1, Add, StLoc0, BrS(0), LdLoc0, Ret),
//        List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret),
//        List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret),
//        List(
//          Nop,
//          Call(MethodDefData(0, 150, "answer", hex"0x000008", List(ParamData(0, 1, "a"), ParamData(0, 2, "b")))),
//          StLoc0,
//          Call(MethodDefData(0, 145, "secretAnswer", hex"0x000008", List(ParamData(0, 1, "a"), ParamData(0, 2, "b")))),
//          StLoc1,
//          LdLoc0,
//          LdLoc1,
//          Call(MethodDefData(0, 150, "sum", hex"0x0002080808", List(ParamData(0, 1, "a"), ParamData(0, 2, "b")))),
//          StLoc2,
//          NewObj(MethodDefData(0, 6278, ".ctor", hex"0x200001", List())),
//          StLoc3,
//          LdLoc3,
//          CallVirt(MethodDefData(0, 134, "personalAnswer", hex"0x200008", List())),
//          StLocS(4),
//          LdLoc3,
//          CallVirt(MethodDefData(0, 129, "personalSecretAnswer", hex"0x200008", List())),
//          StLocS(5),
//          Ret
//        ),
//        List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret)
//      )
    }
  }
}
