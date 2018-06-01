package pravda.dotnet

import pravda.dotnet.CIL._
import pravda.dotnet.Signatures.SigType._
import pravda.dotnet.Signatures._
import pravda.dotnet.TablesData._
import utest._

object IfTests extends TestSuite {

  val tests = Tests {
    'ifParse - {
      val Right((_, cilData, methods, signatures)) = FileParser.parseFile("if.exe")
      methods ==> List(
        Method(
          List(
            Nop,
            LdSFld(FieldData(22, "x", 32)),
            LdcI41,
            Clt,
            StLoc0,
            LdLoc0,
            BrFalseS(8),
            Nop,
            LdcI44,
            StSFld(FieldData(22, "x", 32)),
            Nop,
            LdSFld(FieldData(22, "x", 32)),
            LdcI45,
            Cgt,
            StLoc1,
            LdLoc1,
            BrFalseS(22),
            Nop,
            LdSFld(FieldData(22, "x", 32)),
            LdcI46,
            Cgt,
            StLoc2,
            LdLoc2,
            BrFalseS(8),
            Nop,
            LdcI47,
            StSFld(FieldData(22, "x", 32)),
            Nop,
            Nop,
            LdSFld(FieldData(22, "x", 32)),
            LdcI40,
            Cgt,
            StLoc3,
            LdLoc3,
            BrFalseS(10),
            Nop,
            LdcI44,
            StSFld(FieldData(22, "x", 32)),
            Nop,
            BrS(8),
            Nop,
            LdcI45,
            StSFld(FieldData(22, "x", 32)),
            Nop,
            Ret
          ),
          2,
          Some(16)
        ),
        Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
        Method(List(LdcI41, StSFld(FieldData(22, "x", 32)), Ret), 0, None)
      )

      signatures.toList.sortBy(_._1) ==> List(
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
           List(LocalVar(Boolean, false),
                LocalVar(Boolean, false),
                LocalVar(Boolean, false),
                LocalVar(Boolean, false)))),
        (32, FieldSig(I4)),
        (35, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
      )

    }
  }
}
