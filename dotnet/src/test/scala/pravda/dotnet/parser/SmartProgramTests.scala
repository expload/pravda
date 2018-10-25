package pravda.dotnet

package parser

import pravda.common.TestUtils
import pravda.dotnet.DotnetCompilation.dsl._
import pravda.dotnet.data.Heaps.SequencePoint
import pravda.dotnet.data.TablesData._
import utest._

object SmartProgramTests extends TestSuite {

  val tests = Tests {
    'SmartProgram - {
      val Right(files) =
        steps(
          "Pravda.dll" -> Seq("PravdaDotNet/Pravda.cs"),
          "SmartProgram.exe" -> Seq("Pravda.dll", "dotnet-tests/resources/SmartProgram.cs")
        ).run

      val pe = files.last.parsedPe
      val pdb = files.last.parsedPdb.get
      val src = "/tmp/pravda/SmartProgram.cs"

      TestUtils.assertEqual(
        pe.cilData.tables.customAttributeTable,
        Vector(
          CustomAttributeData(Ignored,
                              MemberRefData(TypeRefData(6,
                                                        "CompilationRelaxationsAttribute",
                                                        "System.Runtime.CompilerServices"),
                                            ".ctor",
                                            1)),
          CustomAttributeData(Ignored,
                              MemberRefData(TypeRefData(6,
                                                        "RuntimeCompatibilityAttribute",
                                                        "System.Runtime.CompilerServices"),
                                            ".ctor",
                                            6)),
          CustomAttributeData(Ignored,
                              MemberRefData(TypeRefData(6, "DebuggableAttribute", "System.Diagnostics"), ".ctor", 10)),
          CustomAttributeData(
            TypeDefData(
              1,
              1048577,
              "SmartProgram",
              "",
              TypeRefData(6, "Object", "System"),
              Vector(FieldData(1, "Balances", 64)),
              Vector(
                MethodDefData(0, 0, 134, "BalanceOf", 73, Vector(ParamData(0, 1, "tokenOwner"))),
                MethodDefData(1, 0, 134, "Transfer", 79, Vector(ParamData(0, 1, "to"), ParamData(0, 2, "tokens"))),
                MethodDefData(2, 0, 150, "Main", 86, Vector()),
                MethodDefData(3, 0, 6278, ".ctor", 6, Vector())
              )
            ),
            MemberRefData(TypeRefData(10, "Program", "Expload.Pravda"), ".ctor", 6)
          )
        )
      )

      TestUtils.assertEqual(
        pdb.tablesData.methodDebugInformationTable,
        Vector(
          MethodDebugInformationData(Some(src),
                                     List(SequencePoint(0, 10, 5, 10, 6),
                                          SequencePoint(1, 11, 9, 11, 51),
                                          SequencePoint(17, 12, 5, 12, 6))),
          MethodDebugInformationData(
            Some(src),
            List(
              SequencePoint(0, 15, 5, 15, 6),
              SequencePoint(1, 16, 9, 16, 24),
              SequencePoint(9, 16, 25, 16, 26),
              SequencePoint(10, 17, 13, 17, 65),
              SequencePoint(37, 17, 66, 17, 67),
              SequencePoint(38, 18, 17, 18, 93),
              SequencePoint(74, 19, 17, 19, 71),
              SequencePoint(102, 20, 13, 20, 14),
              SequencePoint(103, 21, 9, 21, 10),
              SequencePoint(104, 22, 5, 22, 6)
            )
          ),
          MethodDebugInformationData(Some(src),
                                     List(SequencePoint(0, 24, 31, 24, 32), SequencePoint(1, 24, 32, 24, 33))),
          MethodDebugInformationData(Some(src), List(SequencePoint(0, 7, 5, 7, 62)))
        )
      )
    }
  }
}
