package pravda.dotnet
package parsers

import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._
import utest._

object ObjectsTests extends TestSuite {

  val tests = Tests {
    'objectsParse - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("objects.exe")
      methods ==> List(
        Method(List(LdArg0,
                    Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)),
                    Nop,
                    Nop,
                    LdArg0,
                    LdArg1,
                    StFld(FieldData(1, "a", 37)),
                    Ret),
               0,
               None),
        Method(List(Nop, LdArg0, LdFld(FieldData(1, "a", 37)), LdcI4S(42), Add, StLoc0, BrS(0), LdLoc0, Ret),
               2,
               Some(16)),
        Method(List(LdArg0,
                    Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)),
                    Nop,
                    Nop,
                    LdArg0,
                    LdArg1,
                    StFld(FieldData(1, "b", 37)),
                    Ret),
               0,
               None),
        Method(List(Nop, LdArg0, LdFld(FieldData(1, "b", 37)), LdcI4S(42), Add, StLoc0, BrS(0), LdLoc0, Ret),
               2,
               Some(16)),
        Method(
          List(
            Nop,
            LdcI4S(-42),
            NewObj(MethodDefData(0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "_a")))),
            StLoc0,
            LdcI40,
            NewObj(MethodDefData(0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "_b")))),
            StLoc1,
            LdLoc0,
            CallVirt(MethodDefData(0, 134, "answerA", 40, Vector())),
            LdLoc1,
            CallVirt(MethodDefData(0, 134, "answerB", 40, Vector())),
            Add,
            StLoc2,
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
             LocalVar(
               Cls(TypeDefData(
                 1048577,
                 "A",
                 "",
                 Ignored,
                 Vector(FieldData(1, "a", 37)),
                 Vector(MethodDefData(0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "_a"))),
                        MethodDefData(0, 134, "answerA", 40, Vector()))
               )),
               false
             ),
             LocalVar(
               Cls(TypeDefData(
                 1048577,
                 "B",
                 "",
                 Ignored,
                 Vector(FieldData(1, "b", 37)),
                 Vector(MethodDefData(0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "_b"))),
                        MethodDefData(0, 134, "answerB", 40, Vector()))
               )),
               false
             ),
             LocalVar(I4, false)
           ))),
        (37, FieldSig(I4)),
        (40, MethodRefDefSig(true, false, false, false, 0, Tpe(I4, false), List())),
        (44, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
      )

    }
  }
}
