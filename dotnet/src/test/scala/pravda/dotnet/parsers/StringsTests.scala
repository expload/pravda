package pravda.dotnet.parsers

import pravda.common.DiffUtils
import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures._
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._
import utest._

object StringsTests extends TestSuite {

  val tests = Tests {
    'stringsParse - {
      val Right((_, cilData, methods, signatures)) = FileParser.parseFile("strings.exe")

      DiffUtils.assertEqual(
        methods,
        List(
          Method(
            List(
              Nop,
              LdStr("zapupu"),
              StLoc0,
              LdStr("lu"),
              StLoc1,
              LdStr("pa"),
              StLoc2,
              LdLoc1,
              LdLoc2,
              Call(MemberRefData(TypeRefData(6, "String", "System"), "Concat", 28)),
              StLoc3,
              LdArg0,
              LdFld(FieldData(6, "strings", 102)),
              LdLoc3,
              LdLoc0,
              CallVirt(MemberRefData(TypeSpecData(34), "put", 41)),
              Nop,
              LdArg0,
              LdFld(FieldData(6, "strings", 102)),
              LdStr("lupa"),
              CallVirt(MemberRefData(TypeSpecData(34), "exists", 49)),
              StLocS(8),
              LdLocS(8),
              BrFalseS(24),
              Nop,
              LdArg0,
              LdFld(FieldData(6, "strings", 102)),
              LdStr("pupa"),
              LdStr(""),
              CallVirt(MemberRefData(TypeSpecData(34), "put", 41)),
              Nop,
              Nop,
              LdLoc0,
              LdcI40,
              CallVirt(MemberRefData(TypeRefData(6, "String", "System"), "get_Chars", 55)),
              StLocS(4),
              LdLoc3,
              LdcI43,
              CallVirt(MemberRefData(TypeRefData(6, "String", "System"), "get_Chars", 55)),
              StLocS(5),
              LdLoc3,
              LdcI41,
              LdcI42,
              CallVirt(MemberRefData(TypeRefData(6, "String", "System"), "Substring", 60)),
              StLocS(6),
              LdLoc3,
              LdcI41,
              CallVirt(MemberRefData(TypeRefData(6, "String", "System"), "Substring", 66)),
              StLocS(7),
              Ret
            ),
            3,
            Some(16)
          ),
          Method(List(Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Attribute", "System"), ".ctor", 6)), Nop, Ret),
                 0,
                 None),
          Method(List(), 0, None),
          Method(List(), 0, None),
          Method(List(), 0, None),
          Method(
            List(
              Nop,
              LdArg0,
              LdArg1,
              CallVirt(MemberRefData(TypeSpecData(77), "exists", 49)),
              LdcI40,
              Ceq,
              StLoc0,
              LdLoc0,
              BrFalseS(5),
              Nop,
              LdArg2,
              StLoc1,
              BrS(11),
              Nop,
              LdArg0,
              LdArg1,
              CallVirt(MemberRefData(TypeSpecData(77), "get", 86)),
              StLoc1,
              BrS(0),
              LdLoc1,
              Ret
            ),
            2,
            Some(71)
          ),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None)
        )
      )

      DiffUtils.assertEqual(
        signatures.toList.sortBy(_._1),
        List(
          (1, MethodRefDefSig(true, false, false, false, 0, Tpe(Void, false), List(Tpe(I4, false)))),
          (6, MethodRefDefSig(true, false, false, false, 0, Tpe(Void, false), List())),
          (10,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Void, false),
                           List(Tpe(ValueTpe(TypeRefData(15, "DebuggingModes", "")), false)))),
          (16,
           LocalVarSig(
             List(
               LocalVar(String, false),
               LocalVar(String, false),
               LocalVar(String, false),
               LocalVar(String, false),
               LocalVar(Char, false),
               LocalVar(Char, false),
               LocalVar(String, false),
               LocalVar(String, false),
               LocalVar(Boolean, false)
             ))),
          (28,
           MethodRefDefSig(false,
                           false,
                           false,
                           false,
                           0,
                           Tpe(String, false),
                           List(Tpe(String, false), Tpe(String, false)))),
          (34,
           TypeSig(
             Tpe(
               Generic(
                 Cls(TypeDefData(
                   1048705,
                   "Mapping`2",
                   "io.mytc.pravda",
                   Ignored,
                   Vector(),
                   Vector(
                     MethodDefData(0, 1478, "get", 86, Vector(ParamData(0, 1, "key"))),
                     MethodDefData(0, 1478, "exists", 49, Vector(ParamData(0, 1, "key"))),
                     MethodDefData(0, 1478, "put", 41, Vector(ParamData(0, 1, "key"), ParamData(0, 2, "value"))),
                     MethodDefData(0, 134, "getDefault", 114, Vector(ParamData(0, 1, "key"), ParamData(0, 2, "def"))),
                     MethodDefData(0, 6276, ".ctor", 6, Vector())
                   )
                 )),
                 List(String, String)
               ),
               false
             ))),
          (41,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Void, false),
                           List(Tpe(Var(0), false), Tpe(Var(1), false)))),
          (49, MethodRefDefSig(true, false, false, false, 0, Tpe(Boolean, false), List(Tpe(Var(0), false)))),
          (55, MethodRefDefSig(true, false, false, false, 0, Tpe(Char, false), List(Tpe(I4, false)))),
          (60, MethodRefDefSig(true, false, false, false, 0, Tpe(String, false), List(Tpe(I4, false), Tpe(I4, false)))),
          (66, MethodRefDefSig(true, false, false, false, 0, Tpe(String, false), List(Tpe(I4, false)))),
          (71, LocalVarSig(List(LocalVar(Boolean, false), LocalVar(Var(1), false)))),
          (77,
           TypeSig(
             Tpe(
               Generic(
                 Cls(TypeDefData(
                   1048705,
                   "Mapping`2",
                   "io.mytc.pravda",
                   Ignored,
                   Vector(),
                   Vector(
                     MethodDefData(0, 1478, "get", 86, Vector(ParamData(0, 1, "key"))),
                     MethodDefData(0, 1478, "exists", 49, Vector(ParamData(0, 1, "key"))),
                     MethodDefData(0, 1478, "put", 41, Vector(ParamData(0, 1, "key"), ParamData(0, 2, "value"))),
                     MethodDefData(0, 134, "getDefault", 114, Vector(ParamData(0, 1, "key"), ParamData(0, 2, "def"))),
                     MethodDefData(0, 6276, ".ctor", 6, Vector())
                   )
                 )),
                 List(Var(0), Var(1))
               ),
               false
             ))),
          (86, MethodRefDefSig(true, false, false, false, 0, Tpe(Var(1), false), List(Tpe(Var(0), false)))),
          (102,
           FieldSig(
             Generic(
               Cls(TypeDefData(
                 1048705,
                 "Mapping`2",
                 "io.mytc.pravda",
                 Ignored,
                 Vector(),
                 Vector(
                   MethodDefData(0, 1478, "get", 86, Vector(ParamData(0, 1, "key"))),
                   MethodDefData(0, 1478, "exists", 49, Vector(ParamData(0, 1, "key"))),
                   MethodDefData(0, 1478, "put", 41, Vector(ParamData(0, 1, "key"), ParamData(0, 2, "value"))),
                   MethodDefData(0, 134, "getDefault", 114, Vector(ParamData(0, 1, "key"), ParamData(0, 2, "def"))),
                   MethodDefData(0, 6276, ".ctor", 6, Vector())
                 )
               )),
               List(String, String)
             ))),
          (110, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List())),
          (114,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Var(1), false),
                           List(Tpe(Var(0), false), Tpe(Var(1), false))))
        )
      )
    }
  }
}
