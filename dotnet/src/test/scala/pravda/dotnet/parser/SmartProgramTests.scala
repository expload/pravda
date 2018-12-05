package pravda.dotnet

package parser

import java.nio.file.Paths

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
      val src = Paths.get(DotnetCompilation.pravdaDir.toString, "SmartProgram.cs").toString

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
              Vector(FieldData(0, 1, "Balances", 55)),
              Vector(
                MethodDefData(0, 0, 134, "BalanceOf", 64, Vector(ParamData(0, 1, "tokenOwner"))),
                MethodDefData(1, 0, 134, "Transfer", 70, Vector(ParamData(0, 1, "to"), ParamData(0, 2, "tokens"))),
                MethodDefData(2, 0, 134, "Emit", 70, Vector(ParamData(0, 1, "owner"), ParamData(0, 2, "tokens"))),
                MethodDefData(3, 0, 150, "Main", 77, Vector()),
                MethodDefData(4, 0, 6278, ".ctor", 6, Vector())
              )
            ),
            MemberRefData(TypeRefData(10, "Program", "Expload.Pravda"), ".ctor", 6)
          )
        )
      )

      TestUtils.assertEqual(
        pdb.tablesData.methodDebugInformationTable,
        Vector(
          MethodDebugInformationData(Some(src), List(SequencePoint(0, 11, 9, 11, 53))),
          MethodDebugInformationData(
            Some(src),
            List(SequencePoint(0, 16, 9, 16, 24),
                 SequencePoint(4, 17, 13, 17, 67),
                 SequencePoint(24, 18, 17, 18, 92),
                 SequencePoint(59, 19, 17, 19, 70),
                 SequencePoint(86, 22, 5, 22, 6))
          ),
          MethodDebugInformationData(Some(src),
                                     List(SequencePoint(0, 26, 9, 26, 24),
                                          SequencePoint(4, 27, 13, 27, 72),
                                          SequencePoint(31, 29, 5, 29, 6))),
          MethodDebugInformationData(Some(src), List(SequencePoint(0, 31, 32, 31, 33))),
          MethodDebugInformationData(Some(src), List(SequencePoint(0, 7, 5, 7, 70)))
        )
      )
    }
  }
}
