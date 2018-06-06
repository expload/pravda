package pravda.dotnet.parsers

import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._
import utest._

object MethodCallingTests extends TestSuite {

  val tests = Tests {
    'methodCallingParse - {
      val Right((_, cilData, methods, signatures)) = FileParser.parseFile("method_calling.exe")
      methods ==> List(
        Method(List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret), 1, Some(16)),
        Method(List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret), 1, Some(16)),
        Method(List(Nop, LdArg0, LdArg1, Add, StLoc0, BrS(0), LdLoc0, Ret), 2, Some(16)),
        Method(List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret), 1, Some(16)),
        Method(List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret), 1, Some(16)),
        Method(
          List(
            Nop,
            Call(MethodDefData(0, 150, "answer", 39, List())),
            StLoc0,
            Call(MethodDefData(0, 145, "secretAnswer", 39, List())),
            StLoc1,
            LdLoc0,
            LdLoc1,
            Call(MethodDefData(0, 150, "sum", 43, List(ParamData(0, 1, "a"), ParamData(0, 2, "b")))),
            StLoc2,
            NewObj(MethodDefData(0, 6278, ".ctor", 6, List())),
            StLoc3,
            LdLoc3,
            CallVirt(MethodDefData(0, 134, "personalAnswer", 49, List())),
            StLocS(4),
            LdLoc3,
            CallVirt(MethodDefData(0, 129, "personalSecretAnswer", 49, List())),
            StLocS(5),
            Ret
          ),
          2,
          Some(20)
        ),
        Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None)
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
        (16, LocalVarSig(List(LocalVar(I4, false)))),
        (20,
         LocalVarSig(
           List(
             LocalVar(I4, false),
             LocalVar(I4, false),
             LocalVar(I4, false),
             LocalVar(
               Cls(TypeDefData(1048577, "Program", "", Ignored, List(), List(MethodDefData(0, 150, "answer", 39, List())))),
               false),
             LocalVar(I4, false),
             LocalVar(I4, false)
           ))),
        (39, MethodRefDefSig(false, false, false, false, 0, Tpe(I4, false), List())),
        (43, MethodRefDefSig(false, false, false, false, 0, Tpe(I4, false), List(Tpe(I4, false), Tpe(I4, false)))),
        (49, MethodRefDefSig(true, false, false, false, 0, Tpe(I4, false), List())),
        (53, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
      )

    }
  }
}
