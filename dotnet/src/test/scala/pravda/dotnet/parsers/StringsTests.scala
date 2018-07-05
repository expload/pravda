package pravda.dotnet.parsers

import pravda.common.DiffUtils
import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
//import pravda.dotnet.parsers.Signatures.SigType._
//import pravda.dotnet.parsers.Signatures._
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
              Call(MemberRefData(TypeRefData(6, "String", "System"), "Concat", 24)),
              StLoc3,
              LdArg0,
              LdFld(FieldData(6, "strings", 82)),
              LdLoc3,
              LdLoc0,
              CallVirt(MemberRefData(TypeSpecData(30), "put", 37)),
              Nop,
              LdArg0,
              LdFld(FieldData(6, "strings", 82)),
              LdStr("lupa"),
              CallVirt(MemberRefData(TypeSpecData(30), "exists", 45)),
              StLocS(4),
              LdLocS(4),
              BrFalseS(24),
              Nop,
              LdArg0,
              LdFld(FieldData(6, "strings", 82)),
              LdStr("pupa"),
              LdStr(""),
              CallVirt(MemberRefData(TypeSpecData(30), "put", 37)),
              Nop,
              Nop,
              Ret
            ),
            3,
            Some(16)
          ),
          Method(List(Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Attribute", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(), 0, None),
          Method(List(), 0, None),
          Method(List(), 0, None),
          Method(
            List(
              Nop,
              LdArg0,
              LdArg1,
              CallVirt(MemberRefData(TypeSpecData(57), "exists", 45)),
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
              CallVirt(MemberRefData(TypeSpecData(57), "get", 66)),
              StLoc1,
              BrS(0),
              LdLoc1,
              Ret
            ),
            2,
            Some(51)
          ),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None)
        )
      )
    }
  }
}
