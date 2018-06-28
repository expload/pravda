package pravda.dotnet.parsers

import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._
import utest._

object LoopTests extends TestSuite {

  val tests = Tests {
    'loopParse - {
      val Right((_, cilData, methods, signatures)) = FileParser.parseFile("loop.exe")

      methods ==> List(
        Method(
          List(
            Nop,
            LdcI40,
            StLoc0,
            LdcI40,
            StLoc1,
            BrS(10),
            Nop,
            LdLoc0,
            LdcI42,
            Add,
            StLoc0,
            Nop,
            LdLoc1,
            LdcI41,
            Add,
            StLoc1,
            LdLoc1,
            LdcI4S(10),
            Clt,
            StLoc2,
            LdLoc2,
            BrTrueS(-19),
            BrS(6),
            Nop,
            LdLoc0,
            LdcI42,
            Mull,
            StLoc0,
            Nop,
            LdLoc0,
            LdcI4(10000),
            Clt,
            StLoc3,
            LdLoc3,
            BrTrueS(-18),
            Ret
          ),
          2,
          Some(16)
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
        (16,
         LocalVarSig(
           List(LocalVar(I4, false), LocalVar(I4, false), LocalVar(Boolean, false), LocalVar(Boolean, false)))),
        (32, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
      )
    }
  }
}
