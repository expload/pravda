package pravda.dotnet

package parser

import pravda.common.TestUtils
import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parser.CIL._
import pravda.dotnet.parser.Signatures.SigType._
import pravda.dotnet.parser.Signatures._
import utest._

object MethodCallingTests extends TestSuite {

  val tests = Tests {
    'methodCallingParse - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("method_calling.exe")
      TestUtils.assertEqual(
        methods,
        List(
          Method(List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret), 1, Some(16)),
          Method(List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret), 1, Some(16)),
          Method(List(Nop, LdArg0, LdArg1, Add, StLoc0, BrS(0), LdLoc0, Ret), 2, Some(16)),
          Method(List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret), 1, Some(16)),
          Method(List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret), 1, Some(16)),
          Method(
            List(
              Nop,
              Call(MethodDefData(0, 0, 150, "answer", 39, Vector())),
              StLoc0,
              Call(MethodDefData(1, 0, 145, "secretAnswer", 39, Vector())),
              StLoc1,
              LdLoc0,
              LdLoc1,
              Call(MethodDefData(2, 0, 150, "sum", 43, Vector(ParamData(0, 1, "a"), ParamData(0, 2, "b")))),
              StLoc2,
              NewObj(MethodDefData(6, 0, 6278, ".ctor", 6, Vector())),
              StLoc3,
              LdLoc3,
              CallVirt(MethodDefData(3, 0, 134, "personalAnswer", 49, Vector())),
              StLocS(4),
              LdLoc3,
              CallVirt(MethodDefData(4, 0, 129, "personalSecretAnswer", 49, Vector())),
              StLocS(5),
              Ret
            ),
            2,
            Some(20)
          ),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None)
        )
      )

      TestUtils.assertEqual(
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
          (16, LocalVarSig(List(LocalVar(I4, false)))),
          (20,
           LocalVarSig(
             List(
               LocalVar(I4, false),
               LocalVar(I4, false),
               LocalVar(I4, false),
               LocalVar(
                 Cls(TypeDefData(
                   1,
                   1048577,
                   "Program",
                   "",
                   TypeRefData(6, "Object", "System"),
                   Vector(),
                   Vector(
                     MethodDefData(0, 0, 150, "answer", 39, Vector()),
                     MethodDefData(1, 0, 145, "secretAnswer", 39, Vector()),
                     MethodDefData(2, 0, 150, "sum", 43, Vector(ParamData(0, 1, "a"), ParamData(0, 2, "b"))),
                     MethodDefData(3, 0, 134, "personalAnswer", 49, Vector()),
                     MethodDefData(4, 0, 129, "personalSecretAnswer", 49, Vector()),
                     MethodDefData(5, 0, 150, "Main", 53, Vector()),
                     MethodDefData(6, 0, 6278, ".ctor", 6, Vector())
                   )
                 )),
                 false
               ),
               LocalVar(I4, false),
               LocalVar(I4, false)
             ))),
          (39, MethodRefDefSig(false, false, false, false, 0, Tpe(I4, false), List())),
          (43, MethodRefDefSig(false, false, false, false, 0, Tpe(I4, false), List(Tpe(I4, false), Tpe(I4, false)))),
          (49, MethodRefDefSig(true, false, false, false, 0, Tpe(I4, false), List())),
          (53, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
        )
      )

    }
  }
}
