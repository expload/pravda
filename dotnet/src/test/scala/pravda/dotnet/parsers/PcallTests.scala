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
              LdcI44,
              NewArr(TypeRefData(6, "Byte", "System")),
              Dup,
              LdToken(FieldData(307, "12DADA1FFF4D4787ADE3333147202C3B443E376F", 63)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              21)),
              NewObj(MemberRefData(TypeRefData(10, "Bytes", "Com.Expload"), ".ctor", 29)),
              Call(MethodSpecData(MemberRefData(TypeRefData(10, "ProgramHelper", "Com.Expload"), "Program", 35), 43)),
              LdcI4S(10),
              LdcI4S(20),
              CallVirt(MemberRefData(TypeRefData(14, "MyAnotherProgram", "Com.Expload.Programs"), "Add", 48)),
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
          (21,
           MethodRefDefSig(
             false,
             false,
             false,
             false,
             0,
             Tpe(Void, false),
             List(Tpe(Cls(TypeRefData(6, "Array", "System")), false),
                  Tpe(ValueTpe(TypeRefData(6, "RuntimeFieldHandle", "System")), false))
           )),
          (29,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Void, false),
                           List(Tpe(Arr(U1, ArrayShape(1, List(), List())), false)))),
          (35,
           MethodRefDefSig(false,
                           false,
                           false,
                           false,
                           1,
                           Tpe(MVar(0), false),
                           List(Tpe(Cls(TypeRefData(10, "Bytes", "Com.Expload")), false)))),
          (48, MethodRefDefSig(true, false, false, false, 0, Tpe(I4, false), List(Tpe(I4, false), Tpe(I4, false)))),
          (63, FieldSig(I4)),
          (66, MethodRefDefSig(true, false, false, false, 0, Tpe(I4, false), List())),
          (70, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
        )
      )
    }
  }
}
