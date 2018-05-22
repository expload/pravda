package io.mytc.sood.cil

import fastparse.byte.all._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.Signatures.Info.SigType._
import io.mytc.sood.cil.Signatures.Info._
import io.mytc.sood.cil.TablesData._
import utest._

object SmartProgramTests extends TestSuite {

  val tests = Tests {
    'smartProgramParse - {
      val Right((_, cilData, methods)) = PeParsersUtils.parsePe("smart_program.exe")

      cilData.tables ==> TablesData(
        List(FieldData(1, "balances", hex"0x0615121402121808"), FieldData(1, "sender", hex"0x061218")),
        List(
          MemberRefData(9, ".ctor", hex"0x20010108"),
          MemberRefData(17, ".ctor", hex"0x200001"),
          MemberRefData(25, ".ctor", hex"0x2001011111"),
          MemberRefData(12, "get", hex"0x200113011300"),
          MemberRefData(12, "getDefault", hex"0x2002130113001301"),
          MemberRefData(12, "put", hex"0x20020113001301"),
          MemberRefData(41, ".ctor", hex"0x200001"),
          MemberRefData(49, ".ctor", hex"0x200001")
        ),
        List(
          MethodDefData(0, 134, "balanceOf", hex"0x2001081218", List(ParamData(0, 1, "tokenOwner"))),
          MethodDefData(0,
                        134,
                        "transfer",
                        hex"0x200201121808",
                        List(ParamData(0, 1, "to"), ParamData(0, 2, "tokens"))),
          MethodDefData(0, 6278, ".ctor", hex"0x200001", List()),
          MethodDefData(0, 150, "Main", hex"0x000001", List()),
          MethodDefData(0, 6278, ".ctor", hex"0x200001", List()),
          MethodDefData(0, 6278, ".ctor", hex"0x200001", List()),
          MethodDefData(0, 1478, "get", hex"0x200113011300", List(ParamData(0, 1, "key"))),
          MethodDefData(0, 1478, "exists", hex"0x2001021300", List(ParamData(0, 1, "key"))),
          MethodDefData(0, 1478, "put", hex"0x20020113001301", List(ParamData(0, 1, "key"), ParamData(0, 2, "value"))),
          MethodDefData(0,
                        1478,
                        "getDefault",
                        hex"0x2002130113001301",
                        List(ParamData(0, 1, "key"), ParamData(0, 2, "def"))),
          MethodDefData(0, 6278, ".ctor", hex"0x200001", List()),
          MethodDefData(0, 6278, ".ctor", hex"0x200001", List()),
          MethodDefData(0, 6278, ".ctor", hex"0x200001", List())
        ),
        List(
          ParamData(0, 1, "tokenOwner"),
          ParamData(0, 1, "to"),
          ParamData(0, 2, "tokens"),
          ParamData(0, 1, "key"),
          ParamData(0, 1, "key"),
          ParamData(0, 1, "key"),
          ParamData(0, 2, "value"),
          ParamData(0, 1, "key"),
          ParamData(0, 2, "def")
        ),
        List(
          TypeDefData(
            0,
            "<Module>",
            "",
            Ignored,
            List(),
            List()
          ),
          TypeDefData(
            1048576,
            "MyProgram",
            "",
            Ignored,
            List(FieldData(1, "balances", hex"0x0615121402121808"), FieldData(1, "sender", hex"0x061218")),
            List(
              MethodDefData(0, 134, "balanceOf", hex"0x2001081218", List(ParamData(0, 1, "tokenOwner"))),
              MethodDefData(0,
                            134,
                            "transfer",
                            hex"0x200201121808",
                            List(ParamData(0, 1, "to"), ParamData(0, 2, "tokens"))),
              MethodDefData(0, 6278, ".ctor", hex"0x200001", List())
            )
          ),
          TypeDefData(
            1048576,
            "MainClass",
            "",
            Ignored,
            List(),
            List(MethodDefData(0, 150, "Main", hex"0x000001", List()),
                 MethodDefData(0, 6278, ".ctor", hex"0x200001", List()))
          ),
          TypeDefData(1048577,
                      "Program",
                      "io.mytc.pravda",
                      Ignored,
                      List(),
                      List(MethodDefData(0, 6278, ".ctor", hex"0x200001", List()))),
          TypeDefData(
            161,
            "Mapping`2",
            "io.mytc.pravda",
            Ignored,
            List(),
            List(
              MethodDefData(0, 1478, "get", hex"0x200113011300", List(ParamData(0, 1, "key"))),
              MethodDefData(0, 1478, "exists", hex"0x2001021300", List(ParamData(0, 1, "key"))),
              MethodDefData(0,
                            1478,
                            "put",
                            hex"0x20020113001301",
                            List(ParamData(0, 1, "key"), ParamData(0, 2, "value"))),
              MethodDefData(0,
                            1478,
                            "getDefault",
                            hex"0x2002130113001301",
                            List(ParamData(0, 1, "key"), ParamData(0, 2, "def")))
            )
          ),
          TypeDefData(1048577,
                      "Address",
                      "io.mytc.pravda",
                      Ignored,
                      List(),
                      List(MethodDefData(0, 6278, ".ctor", hex"0x200001", List()))),
          TypeDefData(1048577,
                      "Data",
                      "io.mytc.pravda",
                      Ignored,
                      List(),
                      List(MethodDefData(0, 6278, ".ctor", hex"0x200001", List()))),
          TypeDefData(1048577, "Word", "io.mytc.pravda", Ignored, List(), List())
        ),
        List(StandAloneSigData(LocalVarSig(Seq(LocalVar(I4, false)))),
             StandAloneSigData(LocalVarSig(Seq(LocalVar(Boolean, false)))))
      )
      println(methods)
      methods ==> List(
        Method(
          List(Nop,
               LdArg0,
               LdFld(FieldData(1, "balances", hex"0x0615121402121808")),
               LdArg1,
               CallVirt(MemberRefData(12, "get", hex"0x200113011300")),
               StLoc0,
               BrS(0),
               LdLoc0,
               Ret),
          2,
          LocalVarSig(Seq(LocalVar(I4, false)))
        ),
        Method(
          List(
            Nop,
            LdArg0,
            LdFld(FieldData(1, "balances", hex"0x0615121402121808")),
            LdArg0,
            LdFld(FieldData(1, "sender", hex"0x061218")),
            LdcI40,
            CallVirt(MemberRefData(12, "getDefault", hex"0x2002130113001301")),
            LdArg2,
            Clt,2
            LdcI40,
            Ceq,
            StLoc0,
            LdLoc0,
            BrFalseS(68),
            Nop,
            LdArg0,
            LdFld(FieldData(1, balances, hex"0x0615121402121808")),
            LdArg0,
            LdFld(FieldData(1, sender, hex"0x061218")),
            LdArg0,
            LdFld(FieldData(1, balances, hex"0x0615121402121808")),
            LdArg0,
            LdFld(FieldData(1, sender, hex"0x061218")),
            LdcI40,
            CallVirt(MemberRefData(12, getDefault, hex"0x2002130113001301")),
            LdArg2,
            Sub,
            CallVirt(MemberRefData(12, put, hex"0x20020113001301")),
            Nop,
            LdArg0,
            LdFld(FieldData(1, balances, hex"0x0615121402121808")),
            LdArg1,
            LdArg0,
            LdFld(FieldData(1, balances, hex"0x0615121402121808")),
            LdArg1,
            LdcI40,
            CallVirt(MemberRefData(12, getDefault, hex"0x2002130113001301")),
            LdArg2,
            Add,
            CallVirt(MemberRefData(12, put, hex"0x20020113001301")),
            Nop,
            Nop,
            Ret
          ),
          5,
          LocalVarSig(Seq(LocalVar(Boolean, false)))
        ),
        Method(
          List(
            LdArg0,
            LdNull,
            StFld(FieldData(1, balances, hex"0x0615121402121808")),
            LdArg0,
            LdNull,
            StFld(FieldData(1, sender, hex"0x061218")),
            LdArg0,
            Call(MemberRefData(41, ".ctor", hex"0x200001")),
            Nop,
            Ret
          ),
          0,
          LocalVarSig(List())
        ),
        Method(List(Nop, Ret), 0, LocalVarSig(List())),
        Method(List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret), 0, LocalVarSig(List())),
        Method(List(LdArg0, Call(MemberRefData(49, ".ctor", hex"0x200001")), Nop, Ret), 0, LocalVarSig(List())),
        Method(List(), 0, LocalVarSig(List())),
        Method(List(), 0, LocalVarSig(List())),
        Method(List(), 0, LocalVarSig(List())),
        Method(List(), 0, LocalVarSig(List())),
        Method(List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret), 0, LocalVarSig(List())),
        Method(List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret), 0, LocalVarSig(List())),
        Method(List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret), 0, LocalVarSig(List()))
      )
    }
  }
}
