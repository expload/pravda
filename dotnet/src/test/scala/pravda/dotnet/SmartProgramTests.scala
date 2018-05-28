package pravda.dotnet

import pravda.dotnet.CIL._
import pravda.dotnet.Signatures.SigType._
import pravda.dotnet.Signatures._
import pravda.dotnet.TablesData._
import utest._

object SmartProgramTests extends TestSuite {

  val tests = Tests {
    'smartProgramParse - {
      val Right((_, cilData, methods, signatures)) = PeParsersUtils.parsePe("smart_program.exe")

      cilData.tables ==> TablesData(
        List(FieldData(1, "balances", 65L), FieldData(1, "sender", 74L)),
        List(
          MemberRefData(9, ".ctor", 1L),
          MemberRefData(17, ".ctor", 6L),
          MemberRefData(25, ".ctor", 10L),
          MemberRefData(12, "get", 28L),
          MemberRefData(12, "getDefault", 39L),
          MemberRefData(12, "put", 48L),
          MemberRefData(41, ".ctor", 6L),
          MemberRefData(49, ".ctor", 6L)
        ),
        List(
          MethodDefData(0, 134, "balanceOf", 78L, List(ParamData(0, 1, "tokenOwner"))),
          MethodDefData(0, 134, "transfer", 84L, List(ParamData(0, 1, "to"), ParamData(0, 2, "tokens"))),
          MethodDefData(0, 6278, ".ctor", 6L, List()),
          MethodDefData(0, 150, "Main", 91L, List()),
          MethodDefData(0, 6278, ".ctor", 6L, List()),
          MethodDefData(0, 6278, ".ctor", 6L, List()),
          MethodDefData(0, 1478, "get", 28L, List(ParamData(0, 1, "key"))),
          MethodDefData(0, 1478, "exists", 95L, List(ParamData(0, 1, "key"))),
          MethodDefData(0, 1478, "put", 48L, List(ParamData(0, 1, "key"), ParamData(0, 2, "value"))),
          MethodDefData(0, 1478, "getDefault", 39L, List(ParamData(0, 1, "key"), ParamData(0, 2, "def"))),
          MethodDefData(0, 6278, ".ctor", 6L, List()),
          MethodDefData(0, 6278, ".ctor", 6L, List()),
          MethodDefData(0, 6278, ".ctor", 6L, List())
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
            List(FieldData(1, "balances", 65L), FieldData(1, "sender", 74L)),
            List(
              MethodDefData(0, 134, "balanceOf", 78L, List(ParamData(0, 1, "tokenOwner"))),
              MethodDefData(0, 134, "transfer", 84L, List(ParamData(0, 1, "to"), ParamData(0, 2, "tokens"))),
              MethodDefData(0, 6278, ".ctor", 6L, List())
            )
          ),
          TypeDefData(
            1048576,
            "MainClass",
            "",
            Ignored,
            List(),
            List(MethodDefData(0, 150, "Main", 91L, List()), MethodDefData(0, 6278, ".ctor", 6L, List()))
          ),
          TypeDefData(1048577,
                      "Program",
                      "io.mytc.pravda",
                      Ignored,
                      List(),
                      List(MethodDefData(0, 6278, ".ctor", 6L, List()))),
          TypeDefData(
            161,
            "Mapping`2",
            "io.mytc.pravda",
            Ignored,
            List(),
            List(
              MethodDefData(0, 1478, "get", 28L, List(ParamData(0, 1, "key"))),
              MethodDefData(0, 1478, "exists", 95L, List(ParamData(0, 1, "key"))),
              MethodDefData(0, 1478, "put", 48L, List(ParamData(0, 1, "key"), ParamData(0, 2, "value"))),
              MethodDefData(0, 1478, "getDefault", 39L, List(ParamData(0, 1, "key"), ParamData(0, 2, "def")))
            )
          ),
          TypeDefData(1048577,
                      "Address",
                      "io.mytc.pravda",
                      Ignored,
                      List(),
                      List(MethodDefData(0, 6278, ".ctor", 6L, List()))),
          TypeDefData(1048577,
                      "Data",
                      "io.mytc.pravda",
                      Ignored,
                      List(),
                      List(MethodDefData(0, 6278, ".ctor", 6L, List()))),
          TypeDefData(1048577, "Word", "io.mytc.pravda", Ignored, List(), List())
        ),
        List(
          TypeRefData(6L, "CompilationRelaxationsAttribute", "System.Runtime.CompilerServices"),
          TypeRefData(6L, "RuntimeCompatibilityAttribute", "System.Runtime.CompilerServices"),
          TypeRefData(6, "DebuggableAttribute", "System.Diagnostics"),
          TypeRefData(15, "DebuggingModes", ""),
          TypeRefData(6, "Object", "System"),
          TypeRefData(6, "Attribute", "System")
        ),
        List(StandAloneSigData(16L), StandAloneSigData(35L))
      )

      methods ==> List(
        Method(
          List(
            Nop,
            LdArg0,
            LdFld(FieldData(1, "balances", 65L)),
            LdArg1,
            CallVirt(MemberRefData(12, "get", 28L)),
            StLoc0,
            BrS(0),
            LdLoc0,
            Ret
          ),
          2,
          Some(16L)
        ),
        Method(
          List(
            Nop,
            LdArg0,
            LdFld(FieldData(1, "balances", 65L)),
            LdArg0,
            LdFld(FieldData(1, "sender", 74L)),
            LdcI40,
            CallVirt(MemberRefData(12, "getDefault", 39L)),
            LdArg2,
            Clt,
            LdcI40,
            Ceq,
            StLoc0,
            LdLoc0,
            BrFalseS(68),
            Nop,
            LdArg0,
            LdFld(FieldData(1, "balances", 65L)),
            LdArg0,
            LdFld(FieldData(1, "sender", 74L)),
            LdArg0,
            LdFld(FieldData(1, "balances", 65L)),
            LdArg0,
            LdFld(FieldData(1, "sender", 74L)),
            LdcI40,
            CallVirt(MemberRefData(12, "getDefault", 39L)),
            LdArg2,
            Sub,
            CallVirt(MemberRefData(12, "put", 48L)),
            Nop,
            LdArg0,
            LdFld(FieldData(1, "balances", 65L)),
            LdArg1,
            LdArg0,
            LdFld(FieldData(1, "balances", 65L)),
            LdArg1,
            LdcI40,
            CallVirt(MemberRefData(12, "getDefault", 39L)),
            LdArg2,
            Add,
            CallVirt(MemberRefData(12, "put", 48L)),
            Nop,
            Nop,
            Ret
          ),
          5,
          Some(35L)
        ),
        Method(
          List(
            LdArg0,
            LdNull,
            StFld(FieldData(1, "balances", 65L)),
            LdArg0,
            LdNull,
            StFld(FieldData(1, "sender", 74L)),
            LdArg0,
            Call(MemberRefData(41, ".ctor", 6L)),
            Nop,
            Ret
          ),
          0,
          None
        ),
        Method(List(Nop, Ret), 0, None),
        Method(List(LdArg0, Call(MemberRefData(41, ".ctor", 6L)), Nop, Ret), 0, None),
        Method(List(LdArg0, Call(MemberRefData(49, ".ctor", 6L)), Nop, Ret), 0, None),
        Method(List(), 0, None),
        Method(List(), 0, None),
        Method(List(), 0, None),
        Method(List(), 0, None),
        Method(List(LdArg0, Call(MemberRefData(41, ".ctor", 6L)), Nop, Ret), 0, None),
        Method(List(LdArg0, Call(MemberRefData(41, ".ctor", 6L)), Nop, Ret), 0, None),
        Method(List(LdArg0, Call(MemberRefData(41, ".ctor", 6L)), Nop, Ret), 0, None)
      )

      signatures ==> Map(
        10 -> MethodRefDefSig(true,
                              false,
                              false,
                              false,
                              0,
                              Tpe(Void, false),
                              List(Tpe(ValueTpe(TypeRefData(15, "DebuggingModes", "")), false))),
        78 -> MethodRefDefSig(
          true,
          false,
          false,
          false,
          0,
          Tpe(I4, false),
          List(
            Tpe(Cls(
                  TypeDefData(1048577,
                              "Address",
                              "io.mytc.pravda",
                              Ignored,
                              List(),
                              List(MethodDefData(0, 6278, ".ctor", 6, List())))),
                false))
        ),
        84 -> MethodRefDefSig(
          true,
          false,
          false,
          false,
          0,
          Tpe(Void, false),
          List(Tpe(Cls(
                     TypeDefData(1048577,
                                 "Address",
                                 "io.mytc.pravda",
                                 Ignored,
                                 List(),
                                 List(MethodDefData(0, 6278, ".ctor", 6, List())))),
                   false),
               Tpe(I4, false))
        ),
        1 -> MethodRefDefSig(true, false, false, false, 0, Tpe(Void, false), List(Tpe(I4, false))),
        74 -> FieldSig(
          Cls(
            TypeDefData(1048577,
                        "Address",
                        "io.mytc.pravda",
                        Ignored,
                        List(),
                        List(MethodDefData(0, 6278, ".ctor", 6, List()))))),
        6 -> MethodRefDefSig(true, false, false, false, 0, Tpe(Void, false), List()),
        28 -> MethodRefDefSig(true, false, false, false, 0, Tpe(Var(1), false), List(Tpe(Var(0), false))),
        65 -> FieldSig(
          Generic(
            Cls(TypeDefData(
              161,
              "Mapping`2",
              "io.mytc.pravda",
              Ignored,
              List(),
              List(
                MethodDefData(0, 1478, "get", 28, List(ParamData(0, 1, "key"))),
                MethodDefData(0, 1478, "exists", 95, List(ParamData(0, 1, "key"))),
                MethodDefData(0, 1478, "put", 48, List(ParamData(0, 1, "key"), ParamData(0, 2, "value"))),
                MethodDefData(0, 1478, "getDefault", 39, List(ParamData(0, 1, "key"), ParamData(0, 2, "def")))
              )
            )),
            List(Cls(
                   TypeDefData(1048577,
                               "Address",
                               "io.mytc.pravda",
                               Ignored,
                               List(),
                               List(MethodDefData(0, 6278, ".ctor", 6, List())))),
                 I4)
          )),
        39 -> MethodRefDefSig(true,
                              false,
                              false,
                              false,
                              0,
                              Tpe(Var(1), false),
                              List(Tpe(Var(0), false), Tpe(Var(1), false))),
        91 -> MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()),
        35 -> LocalVarSig(List(LocalVar(Boolean, false))),
        48 -> MethodRefDefSig(true,
                              false,
                              false,
                              false,
                              0,
                              Tpe(Void, false),
                              List(Tpe(Var(0), false), Tpe(Var(1), false))),
        95 -> MethodRefDefSig(true, false, false, false, 0, Tpe(Boolean, false), List(Tpe(Var(0), false))),
        16 -> LocalVarSig(List(LocalVar(I4, false)))
      )
    }
  }
}
