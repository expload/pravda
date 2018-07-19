package pravda.dotnet
package parsers

import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._
import utest._

object ArithmeticsTests extends TestSuite {

  val tests = Tests {
    'arithmepticParse - {
      val Right((_, _, methods, signatures)) = parseFile("arithmetics.exe")

      methods ==> List(
        Method(
          List(
            Nop,
            LdSFld(FieldData(22, "x", 33)),
            LdcI42,
            Add,
            StLoc0,
            LdSFld(FieldData(22, "x", 33)),
            LdcI42,
            Mul,
            StLoc1,
            LdSFld(FieldData(22, "x", 33)),
            LdcI42,
            Div,
            StLoc2,
            LdSFld(FieldData(22, "x", 33)),
            LdcI42,
            Rem,
            StLoc3,
            LdLoc0,
            LdLoc1,
            Add,
            LdcI4S(42),
            Add,
            LdLoc2,
            Mul,
            LdLoc3,
            Add,
            LdcI4(1337),
            Div,
            StLocS(4),
            Ret
          ),
          2,
          Some(16)
        ),
        Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
        Method(List(LdcI4S(10), StSFld(FieldData(22, "x", 33)), Ret), 0, None)
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
           List(LocalVar(I4, false),
                LocalVar(I4, false),
                LocalVar(I4, false),
                LocalVar(I4, false),
                LocalVar(I4, false)))),
        (33, FieldSig(I4)),
        (36, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
      )

    }
  }
}
