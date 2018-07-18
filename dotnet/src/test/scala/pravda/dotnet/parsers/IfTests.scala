package pravda.dotnet
package parsers

import pravda.common.DiffUtils
import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._
import utest._

object IfTests extends TestSuite {

  val tests = Tests {
    'ifParse - {
      val Right((_, cilData, methods, signatures)) = parseFile("if.exe")

      DiffUtils.assertEqual(
        methods,
        List(
          Method(
            List(
              Nop,
              LdcI4S(10),
              StLoc0,
              LdLoc0,
              LdcI41,
              Clt,
              StLoc1,
              LdLoc1,
              BrFalseS(4),
              Nop,
              LdcI44,
              StLoc0,
              Nop,
              LdLoc0,
              LdcI45,
              Cgt,
              StLoc2,
              LdLoc2,
              BrFalseS(14),
              Nop,
              LdLoc0,
              LdcI46,
              Cgt,
              StLoc3,
              LdLoc3,
              BrFalseS(4),
              Nop,
              LdcI47,
              StLoc0,
              Nop,
              Nop,
              LdLoc0,
              LdcI40,
              Cgt,
              StLocS(4),
              LdLocS(4),
              BrFalseS(6),
              Nop,
              LdcI44,
              StLoc0,
              Nop,
              BrS(4),
              Nop,
              LdcI45,
              StLoc0,
              Nop,
              LdLoc0,
              LdcI42,
              BleS(6),
              LdLoc0,
              LdcI44,
              Clt,
              BrS(1),
              LdcI40,
              StLocS(5),
              LdLocS(5),
              BrFalseS(6),
              Nop,
              LdcI46,
              StLoc0,
              Nop,
              BrS(4),
              Nop,
              LdcI48,
              StLoc0,
              Nop,
              LdLoc0,
              LdcI47,
              BgtS(7),
              LdLoc0,
              LdcI4S(10),
              Cgt,
              BrS(1),
              LdcI41,
              StLocS(6),
              LdLocS(6),
              BrFalseS(6),
              Nop,
              LdcI41,
              StLoc0,
              Nop,
              BrS(4),
              Nop,
              LdcI40,
              StLoc0,
              Nop,
              LdLoc0,
              LdcI41,
              BleS(4),
              LdLoc0,
              LdcI43,
              BltS(7),
              LdLoc0,
              LdcI4S(20),
              Cgt,
              BrS(1),
              LdcI41,
              StLocS(7),
              LdLocS(7),
              BrFalseS(6),
              Nop,
              LdcI42,
              StLoc0,
              Nop,
              BrS(4),
              Nop,
              LdcI43,
              StLoc0,
              Nop,
              Ret
            ),
            2,
            Some(16)
          ),
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
               LocalVar(I4, false),
               LocalVar(Boolean, false),
               LocalVar(Boolean, false),
               LocalVar(Boolean, false),
               LocalVar(Boolean, false),
               LocalVar(Boolean, false),
               LocalVar(Boolean, false),
               LocalVar(Boolean, false)
             ))),
          (36, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
        )
      )
    }
  }
}
