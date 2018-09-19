package pravda.dotnet

package parser

import pravda.common.TestUtils
import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parser.CIL._
import pravda.dotnet.parser.Signatures.SigType._
import pravda.dotnet.parser.Signatures._
import utest._

object InheritanceTests extends TestSuite {

  val tests = Tests {
    'objectsParse - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("inheritance.exe")
      TestUtils.assertEqual(
        methods,
        List(
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Nop, Ret),
                 0,
                 None),
          Method(List(Nop,
                      LdArg0,
                      CallVirt(MethodDefData(2, 0, 454, "Answer", 43, Vector())),
                      LdcI41,
                      Add,
                      StLoc0,
                      BrS(0),
                      LdLoc0,
                      Ret),
                 2,
                 Some(16)),
          Method(List(Nop, LdcI40, StLoc0, BrS(0), LdLoc0, Ret), 1, Some(16)),
          Method(
            List(LdArg0,
                 LdArg1,
                 Call(MethodDefData(0, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "val")))),
                 Nop,
                 Nop,
                 LdArg0,
                 LdArg1,
                 StFld(FieldData(1, "AVal", 40)),
                 Ret),
            0,
            None
          ),
          Method(List(Nop, LdArg0, LdFld(FieldData(1, "AVal", 40)), LdcI4S(42), Add, StLoc0, BrS(0), LdLoc0, Ret),
                 2,
                 Some(16)),
          Method(
            List(LdArg0,
                 LdArg1,
                 Call(MethodDefData(0, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "val")))),
                 Nop,
                 Nop,
                 LdArg0,
                 LdArg1,
                 StFld(FieldData(1, "BVal", 40)),
                 Ret),
            0,
            None
          ),
          Method(List(Nop, LdArg0, LdFld(FieldData(1, "BVal", 40)), LdcI4S(43), Add, StLoc0, BrS(0), LdLoc0, Ret),
                 2,
                 Some(16)),
          Method(
            List(
              Nop,
              LdcI4S(100),
              NewObj(MethodDefData(3, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "aVal")))),
              StLoc0,
              LdcI4(200),
              NewObj(MethodDefData(5, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "bVal")))),
              StLoc1,
              LdLoc0,
              CallVirt(MethodDefData(2, 0, 454, "Answer", 43, Vector())),
              LdLoc1,
              CallVirt(MethodDefData(2, 0, 454, "Answer", 43, Vector())),
              Add,
              StLoc2,
              LdLoc0,
              CallVirt(MethodDefData(1, 0, 454, "AnswerPlus1", 43, Vector())),
              StLoc3,
              LdLoc1,
              CallVirt(MethodDefData(1, 0, 454, "AnswerPlus1", 43, Vector())),
              StLocS(4),
              LdLoc3,
              LdLocS(4),
              Add,
              StLocS(5),
              BrS(0),
              LdLocS(5),
              Ret
            ),
            2,
            Some(20)
          ),
          Method(List(Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None)
        )
      )

      val parentCls =
        TypeDefData(
          1,
          1048577,
          "Parent",
          "",
          TypeRefData(6, "Object", "System"),
          Vector(),
          Vector(
            MethodDefData(0, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "val"))),
            MethodDefData(1, 0, 454, "AnswerPlus1", 43, Vector()),
            MethodDefData(2, 0, 454, "Answer", 43, Vector())
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
               LocalVar(Cls(parentCls), false),
               LocalVar(Cls(parentCls), false),
               LocalVar(I4, false),
               LocalVar(I4, false),
               LocalVar(I4, false),
               LocalVar(I4, false)
             ))),
          (40, FieldSig(I4)),
          (43, MethodRefDefSig(true, false, false, false, 0, Tpe(I4, false), List())),
          (47, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
        )
      )

      TestUtils.assertEqual(
        cilData.tables.typeDefTable,
        Vector(
          TypeDefData(0, 0, "<Module>", "", Ignored, Vector(), Vector()),
          parentCls,
          TypeDefData(
            2,
            1048577,
            "A",
            "",
            parentCls,
            Vector(FieldData(1, "AVal", 40)),
            Vector(MethodDefData(3, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "aVal"))),
                   MethodDefData(4, 0, 198, "Answer", 43, Vector()))
          ),
          TypeDefData(
            3,
            1048577,
            "B",
            "",
            parentCls,
            Vector(FieldData(1, "BVal", 40)),
            Vector(MethodDefData(5, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "bVal"))),
                   MethodDefData(6, 0, 198, "Answer", 43, Vector()))
          ),
          TypeDefData(
            4,
            1048577,
            "MyProgram",
            "",
            TypeRefData(6, "Object", "System"),
            Vector(),
            Vector(MethodDefData(7, 0, 134, "Func", 43, Vector()),
                   MethodDefData(8, 0, 150, "Main", 47, Vector()),
                   MethodDefData(9, 0, 6278, ".ctor", 6, Vector()))
          )
        )
      )

      TestUtils.assertEqual(
        cilData.tables.methodDefTable,
        Vector(
          MethodDefData(0, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "val"))),
          MethodDefData(1, 0, 454, "AnswerPlus1", 43, Vector()),
          MethodDefData(2, 0, 454, "Answer", 43, Vector()),
          MethodDefData(3, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "aVal"))),
          MethodDefData(4, 0, 198, "Answer", 43, Vector()),
          MethodDefData(5, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "bVal"))),
          MethodDefData(6, 0, 198, "Answer", 43, Vector()),
          MethodDefData(7, 0, 134, "Func", 43, Vector()),
          MethodDefData(8, 0, 150, "Main", 47, Vector()),
          MethodDefData(9, 0, 6278, ".ctor", 6, Vector())
        )
      )
    }
  }
}
