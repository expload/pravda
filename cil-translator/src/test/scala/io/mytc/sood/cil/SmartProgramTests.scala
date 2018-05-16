package io.mytc.sood.cil

import fastparse.byte.all._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.TablesData._

import utest._

object SmartProgramTests extends TestSuite {
  val tests = Tests {
    'smartProgramParse - {
      val Right((_, cilData, opCodes)) = PeParsersUtils.parsePe("smart_program.exe")

      cilData.tables ==> TablesData(
        List(FieldData(22, "counter", hex"0x0608"), FieldData(6, "fa", hex"0x0608"), FieldData(6, "fb", hex"0x0608")),
        List(
          MemberRefData(9, ".ctor", hex"0x20010108"),
          MemberRefData(17, ".ctor", hex"0x200001"),
          MemberRefData(25, ".ctor", hex"0x2001011111"),
          MemberRefData(41, ".ctor", hex"0x200001"),
          MemberRefData(49, ".ctor", hex"0x200001")
        ),
        List(
          MethodDefData(0, 6278, ".ctor", hex"0x200001", List(ParamData(0, 1, "a"), ParamData(0, 2, "b"))),
          MethodDefData(0, 150, "doSmth", hex"0x0002080808", List(ParamData(0, 1, "a"), ParamData(0, 2, "b"))),
          MethodDefData(0,
            134,
            "receive",
            hex"0x200301080808",
            List(ParamData(0, 1, "mode"), ParamData(0, 2, "a"), ParamData(0, 3, "b"))),
          MethodDefData(0, 134, "otherFunc", hex"0x2002010808", List(ParamData(0, 1, "arg1"), ParamData(0, 2, "arg2"))),
          MethodDefData(0, 150, "Main", hex"0x000001", List()),
          MethodDefData(0, 6278, ".ctor", hex"0x200001", List()),
          MethodDefData(0, 6289, ".cctor", hex"0x000001", List())
        ),
        List(ParamData(0, 1, "a"),
          ParamData(0, 2, "b"),
          ParamData(0, 1, "mode"),
          ParamData(0, 2, "a"),
          ParamData(0, 3, "b"),
          ParamData(0, 1, "arg1"),
          ParamData(0, 2, "arg2")),
        List(
          TypeDefData(
            0,
            "<Module>",
            "",
            Ignored,
            List(FieldData(22, "counter", hex"0x0608"), FieldData(6, "fa", hex"0x0608")),
            List(MethodDefData(0, 6278, ".ctor", hex"0x200001", List(ParamData(0, 1, "a"), ParamData(0, 2, "b"))))
          ),
          TypeDefData(
            1048577,
            "Program",
            "",
            Ignored,
            List(FieldData(22, "counter", hex"0x0608"), FieldData(6, "fa", hex"0x0608")),
            List(MethodDefData(0, 6278, ".ctor", hex"0x200001", List(ParamData(0, 1, "a"), ParamData(0, 2, "b"))))
          ),
          TypeDefData(
            1048576,
            "MyProgram",
            "",
            Ignored,
            List(FieldData(22, "counter", hex"0x0608"), FieldData(6, "fa", hex"0x0608")),
            List(MethodDefData(0, 150, "doSmth", hex"0x0002080808", List(ParamData(0, 1, "a"), ParamData(0, 2, "b"))))
          )
        )
      )

      opCodes ==> List(
        List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret),
        List(Nop, LdArg0, LdArg1, Add, StLoc0, BrS(0), LdLoc0, Ret),
        List(
          Nop,
          LdArg1,
          StLoc0,
          LdLoc0,
          LdcI41,
          Sub,
          Switch(Seq(2, 13, 24, 40)),
          BrS(66),
          LdArg0,
          LdArg2,
          LdArg3,
          Add,
          StFld(FieldData(6, "fa", hex"0x0608")),
          BrS(55),
          LdArg0,
          LdArg2,
          LdArg3,
          Add,
          StFld(FieldData(6, "fb", hex"0x0608")),
          BrS(44),
          LdArg0,
          LdArg2,
          StFld(FieldData(6, "fa", hex"0x0608")),
          LdArg0,
          LdArg3,
          StFld(FieldData(6, "fb", hex"0x0608")),
          BrS(28),
          LdArg0,
          LdArg2,
          LdArg3,
          Call(MethodDefData(0, 150, "doSmth", hex"0x0002080808", List(ParamData(0, 1, "a"), ParamData(0, 2, "b")))),
          StFld(FieldData(6, "fa", hex"0x0608")),
          LdArg0,
          LdArg2,
          LdArg3,
          Call(MethodDefData(0, 150, "doSmth", hex"0x0002080808", List(ParamData(0, 1, "a"), ParamData(0, 2, "b")))),
          StFld(FieldData(6, "fb", hex"0x0608")),
          BrS(0),
          Ret
        ),
        List(Nop, Ret),
        List(Nop, Ret),
        List(
          LdArg0,
          LdcI40,
          StFld(FieldData(6, "fa", hex"0x0608")),
          LdArg0,
          LdcI40,
          StFld(FieldData(6, "fb", hex"0x0608")),
          LdArg0,
          Call(MemberRefData(49, ".ctor", hex"0x200001")),
          Nop,
          Ret
        ),
        List(LdcI40, StSFld(FieldData(22, "counter", hex"0x0608")), Ret)
      )
    }
  }
}
