package pravda.dotnet

import pravda.dotnet.CIL._
import pravda.dotnet.Signatures.SigType._
import pravda.dotnet.Signatures._
import pravda.dotnet.TablesData._
import utest._

// all *.exe files was compiled by csc *.cs

object MiscTests extends TestSuite {

  val tests = Tests {
    'hello_world_exe - {
      val Right((_, cilData, methods, signatures)) = FileParser.parseFile("hello_world.exe")
      methods ==> List(
        Method(List(Nop,
                    LdStr("Hello World!"),
                    Call(MemberRefData(TypeRefData(6, "Console", "System"), "WriteLine", 16)),
                    Nop,
                    Ret),
               0,
               None),
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
        (16, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List(Tpe(String, false)))),
        (30, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
      )
    }
  }
}
