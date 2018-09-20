package pravda.dotnet.parsers

import pravda.common.DiffUtils
import pravda.dotnet.data.Method
import pravda.dotnet.parsePeFile
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._
import utest._

object PcallTests extends TestSuite {

  val tests = Tests {
    'pcallParse - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("pcall.exe")

      DiffUtils.assertEqual(
        methods,
        List(
          Method(
            List(
              Nop,
              LdSFld(MemberRefData(TypeRefData(10, "Bytes", "Expload.Pravda"), "VOID_ADDRESS", 21)),
              Call(MethodSpecData(MemberRefData(TypeRefData(10, "ProgramHelper", "Expload.Pravda"), "Program", 25), 33)),
              LdcI4S(10),
              LdcI4S(20),
              CallVirt(MemberRefData(TypeRefData(14, "MyAnotherProgram", "Expload.Pravda.Programs"), "Add", 38)),
              StLoc0,
              LdLoc0,
              StLoc1,
              BrS(0),
              LdLoc1,
              Ret
            ),
            3,
            Some(16)
          ),
          Method(List(Nop, Ret), 0, None),
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
          (16, LocalVarSig(List(LocalVar(I4, false), LocalVar(I4, false)))),
          (21, FieldSig(Cls(TypeRefData(10, "Bytes", "Expload.Pravda")))),
          (25,
           MethodRefDefSig(false,
                           false,
                           false,
                           false,
                           1,
                           Tpe(MVar(0), false),
                           List(Tpe(Cls(TypeRefData(10, "Bytes", "Expload.Pravda")), false)))),
          (38, MethodRefDefSig(true, false, false, false, 0, Tpe(I4, false), List(Tpe(I4, false), Tpe(I4, false)))),
          (53, MethodRefDefSig(true, false, false, false, 0, Tpe(I4, false), List())),
          (57, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
        )
      )
    }
  }
}
