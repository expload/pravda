package io.mytc.sood.cil

//import fastparse.byte.all._
//import io.mytc.sood.cil.CIL._
//import io.mytc.sood.cil.TablesData._

import utest._

object ClosureTests extends TestSuite {
  val tests = Tests {
    'closureParse - {
      // FIXME objects aren't implemented yet
//      val Right((_, cilData, opCodes)) = PeParsersUtils.parsePe("closure.exe")
//
//      cilData.tables ==> TablesData(
//        List(FieldData(6, "e", hex"0x0608")),
//        List(
//          MemberRefData(9, ".ctor", hex"0x20010108"),
//          MemberRefData(17, ".ctor", hex"0x200001"),
//          MemberRefData(25, ".ctor", hex"0x2001011111"),
//          MemberRefData(57, ".ctor", hex"0x200001"),
//          MemberRefData(12, ".ctor", hex"0x2002011c18"),
//          MemberRefData(12, "Invoke", hex"0x200113011300"),
//          MemberRefData(41, ".ctor", hex"0x200001")
//        ),
//        List(
//          MethodDefData(0, 150, "Main", hex"0x000001", List(ParamData(0, 1, "x"))),
//          MethodDefData(0, 6278, ".ctor", hex"0x200001", List(ParamData(0, 1, "x"))),
//          MethodDefData(0, 6278, ".ctor", hex"0x200001", List(ParamData(0, 1, "x"))),
//          MethodDefData(0, 131, "<Main>b__0", hex"0x20010808", List(ParamData(0, 1, "x")))
//        ),
//        List(ParamData(0, 1, "x")),
//        List(
//          TypeDefData(
//            0,
//            "<Module>",
//            "",
//            Ignored,
//            List(FieldData(6, "e", hex"0x0608")),
//            List(MethodDefData(0, 150, "Main", hex"0x000001", List(ParamData(0, 1, "x"))),
//              MethodDefData(0, 6278, ".ctor", hex"0x200001", List(ParamData(0, 1, "x"))))
//          ),
//          TypeDefData(
//            1048577,
//            "Program",
//            "",
//            Ignored,
//            List(FieldData(6, "e", hex"0x0608")),
//            List(MethodDefData(0, 150, "Main", hex"0x000001", List(ParamData(0, 1, "x"))),
//              MethodDefData(0, 6278, ".ctor", hex"0x200001", List(ParamData(0, 1, "x"))))
//          ),
//          TypeDefData(1048835, "<>c__DisplayClass0_0", "", Ignored, List(FieldData(6, "e", hex"0x0608")), List())
//        )
//      )
//
//      opCodes ==> List(
//        List(
//          NewObj(MethodDefData(0, 6278, ".ctor", hex"0x200001", List(ParamData(0, 1, "x")))),
//          StLoc0,
//          Nop,
//          LdLoc0,
//          LdcI41,
//          StFld(FieldData(6, "e", hex"0x0608")),
//          LdLoc0,
//          LdFtn(MethodDefData(0, 131, "<Main>b__0", hex"0x20010808", List(ParamData(0, 1, "x")))),
//          NewObj(MemberRefData(12, ".ctor", hex"0x2002011c18")),
//          StLoc1,
//          LdLoc1,
//          LdcI43,
//          CallVirt(MemberRefData(12, "Invoke", hex"0x200113011300")),
//          StLoc2,
//          Ret
//        ),
//        List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret),
//        List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret),
//        List(LdArg1, LdArg0, LdFld(FieldData(6, "e", hex"0x0608")), Add, Ret)
//      )
    }
  }
}
