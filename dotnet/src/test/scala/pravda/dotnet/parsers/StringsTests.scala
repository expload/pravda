package pravda.dotnet

package parsers

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
      val Right((_, cilData, methods, signatures)) = parsePeFile("strings.exe")

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
              Call(MemberRefData(TypeRefData(6, "String", "System"), "Concat", 27)),
              StLoc3,
              LdArg0,
              LdFld(FieldData(6, "strings", 74)),
              LdLoc3,
              LdLoc0,
              CallVirt(MemberRefData(TypeSpecData(33), "put", 40)),
              Nop,
              LdArg0,
              LdFld(FieldData(6, "strings", 74)),
              LdStr("lupa"),
              CallVirt(MemberRefData(TypeSpecData(33), "exists", 48)),
              StLocS(7),
              LdLocS(7),
              BrFalseS(24),
              Nop,
              LdArg0,
              LdFld(FieldData(6, "strings", 74)),
              LdStr("pupa"),
              LdStr(""),
              CallVirt(MemberRefData(TypeSpecData(33), "put", 40)),
              Nop,
              Nop,
              LdLoc0,
              LdcI40,
              CallVirt(MemberRefData(TypeRefData(6, "String", "System"), "get_Chars", 54)),
              StLocS(4),
              LdLoc3,
              LdcI43,
              CallVirt(MemberRefData(TypeRefData(6, "String", "System"), "get_Chars", 54)),
              StLocS(5),
              LdLoc3,
              LdcI41,
              LdcI42,
              CallVirt(MemberRefData(TypeRefData(6, "String", "System"), "Substring", 59)),
              StLocS(6),
              Ret
            ),
            3,
            Some(16)
          ),
          Method(List(Nop, Ret), 0, None),
          Method(
            List(
              LdArg0,
              NewObj(MemberRefData(TypeSpecData(33), ".ctor", 6)),
              StFld(FieldData(6, "strings", 74)),
              LdArg0,
              Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)),
              Nop,
              Ret
            ),
            0,
            None
          )
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
               LocalVar(Boolean, false)
             ))),
          (27,
           MethodRefDefSig(false,
                           false,
                           false,
                           false,
                           0,
                           Tpe(String, false),
                           List(Tpe(String, false), Tpe(String, false)))),
          (33,
           TypeSig(
             Tpe(
               Generic(
                 Cls(TypeRefData(10, "Mapping`2", "Com.Expload")),
                 List(String, String)
               ),
               false
             ))),
          (40,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Void, false),
                           List(Tpe(Var(0), false), Tpe(Var(1), false)))),
          (48, MethodRefDefSig(true, false, false, false, 0, Tpe(Boolean, false), List(Tpe(Var(0), false)))),
          (54, MethodRefDefSig(true, false, false, false, 0, Tpe(Char, false), List(Tpe(I4, false)))),
          (59, MethodRefDefSig(true, false, false, false, 0, Tpe(String, false), List(Tpe(I4, false), Tpe(I4, false)))),
          (74,
           FieldSig(
             Generic(
               Cls(TypeRefData(10, "Mapping`2", "Com.Expload")),
               List(String, String)
             ))),
          (82, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
        )
      )
    }
  }
}
