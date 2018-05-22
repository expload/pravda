package io.mytc.sood.cil
//
//import fastparse.byte.all._
//import io.mytc.sood.cil.CIL._
//import io.mytc.sood.cil.TablesData._

import utest._

object ObjectsTests extends TestSuite {

  val tests = Tests {
    'objectsParse - {
      // FIXME objects aren't implemented yet
 //      val Right((_, cilData, opCodes)) = PeParsersUtils.parsePe("objects.exe")
//
//      cilData.tables ==> TablesData(
//        List(FieldData(1, "a", hex"0x0608"), FieldData(1, "b", hex"0x0608")),
//        List(
//          MemberRefData(9, ".ctor", hex"0x20010108"),
//          MemberRefData(17, ".ctor", hex"0x200001"),
//          MemberRefData(25, ".ctor", hex"0x2001011111"),
//          MemberRefData(41, ".ctor", hex"0x200001")
//        ),
//        List(
//          MethodDefData(0, 6278, ".ctor", hex"0x20010108", List(ParamData(0, 1, "_a"))),
//          MethodDefData(0, 134, "answerA", hex"0x200008", List(ParamData(0, 1, "_b"))),
//          MethodDefData(0, 6278, ".ctor", hex"0x20010108", List(ParamData(0, 1, "_b"))),
//          MethodDefData(0, 134, "answerB", hex"0x200008", List()),
//          MethodDefData(0, 150, "Main", hex"0x000001", List()),
//          MethodDefData(0, 6278, ".ctor", hex"0x200001", List())
//        ),
//        List(ParamData(0, 1, "_a"), ParamData(0, 1, "_b")),
//        List(
//          TypeDefData(
//            0,
//            "<Module>",
//            "",
//            Ignored,
//            List(FieldData(1, "a", hex"0x0608")),
//            List(MethodDefData(0, 6278, ".ctor", hex"0x20010108", List(ParamData(0, 1, "_a"))),
//              MethodDefData(0, 134, "answerA", hex"0x200008", List(ParamData(0, 1, "_b"))))
//          ),
//          TypeDefData(
//            1048577,
//            "A",
//            "",
//            Ignored,
//            List(FieldData(1, "a", hex"0x0608")),
//            List(MethodDefData(0, 6278, ".ctor", hex"0x20010108", List(ParamData(0, 1, "_a"))),
//              MethodDefData(0, 134, "answerA", hex"0x200008", List(ParamData(0, 1, "_b"))))
//          ),
//          TypeDefData(
//            1048577,
//            "B",
//            "",
//            Ignored,
//            List(FieldData(1, "b", hex"0x0608")),
//            List(MethodDefData(0, 6278, ".ctor", hex"0x20010108", List(ParamData(0, 1, "_b"))),
//              MethodDefData(0, 134, "answerB", hex"0x200008", List()))
//          ),
//          TypeDefData(1048577, "Program", "", Ignored, List(), List())
//        )
//      )
//
//      opCodes ==> List(
//        List(LdArg0,
//          Call(MemberRefData(41, ".ctor", hex"0x200001")),
//          Nop,
//          Nop,
//          LdArg0,
//          LdArg1,
//          StFld(FieldData(1, "a", hex"0x0608")),
//          Ret),
//        List(Nop, LdArg0, LdFld(FieldData(1, "a", hex"0x0608")), LdcI4S(42), Add, StLoc0, BrS(0), LdLoc0, Ret),
//        List(LdArg0,
//          Call(MemberRefData(41, ".ctor", hex"0x200001")),
//          Nop,
//          Nop,
//          LdArg0,
//          LdArg1,
//          StFld(FieldData(1, "b", hex"0x0608")),
//          Ret),
//        List(Nop, LdArg0, LdFld(FieldData(1, "b", hex"0x0608")), LdcI4S(42), Add, StLoc0, BrS(0), LdLoc0, Ret),
//        List(
//          Nop,
//          LdcI4S(-42),
//          NewObj(MethodDefData(0, 6278, ".ctor", hex"0x20010108", List(ParamData(0, 1, "_a")))),
//          StLoc0,
//          LdcI40,
//          NewObj(MethodDefData(0, 6278, ".ctor", hex"0x20010108", List(ParamData(0, 1, "_b")))),
//          StLoc1,
//          LdLoc0,
//          CallVirt(MethodDefData(0, 134, "answerA", hex"0x200008", List(ParamData(0, 1, "_b")))),
//          LdLoc1,
//          CallVirt(MethodDefData(0, 134, "answerB", hex"0x200008", List())),
//          Add,
//          StLoc2,
//          Ret
//        ),
//        List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret)
//      )
    }
  }
}
