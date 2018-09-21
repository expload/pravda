package pravda.dotnet

package parser

import pravda.common.TestUtils
import pravda.dotnet.data.Heaps.SequencePoint
import pravda.dotnet.data.TablesData._
import utest._

object SmartProgramTests extends TestSuite {

  val tests = Tests {
    'smartProgramParse - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("smart_program.exe")

      TestUtils.assertEqual(
        cilData.tables.customAttributeTable,
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
              1048576,
              "MyProgram",
              "",
              TypeRefData(6, "Object", "System"),
              Vector(FieldData(1, "balances", 64)),
              Vector(
                MethodDefData(0, 0, 134, "balanceOf", 73, Vector(ParamData(0, 1, "tokenOwner"))),
                MethodDefData(1, 0, 134, "transfer", 79, Vector(ParamData(0, 1, "to"), ParamData(0, 2, "tokens"))),
                MethodDefData(2, 0, 150, "Main", 86, Vector()),
                MethodDefData(3, 0, 6278, ".ctor", 6, Vector())
              )
            ),
            MemberRefData(TypeRefData(10, "Program", "Expload.Pravda"), ".ctor", 6)
          )
        )
      )
    }

    'smartProgramPdbParse - {
      val Right((pdb, tables)) = parsePdbFile("smart_program.pdb")
      val src = "/tmp/pravda/smart_program.cs"

      TestUtils.assertEqual(
        tables.methodDebugInformationTable,
        Vector(
          MethodDebugInformationData(Some(src),
                                     List(SequencePoint(0, 8, 44, 8, 45),
                                          SequencePoint(1, 9, 9, 9, 51),
                                          SequencePoint(17, 10, 5, 10, 6))),
          MethodDebugInformationData(
            Some(src),
            List(
              SequencePoint(0, 12, 48, 12, 49),
              SequencePoint(1, 13, 9, 13, 24),
              SequencePoint(9, 13, 25, 13, 26),
              SequencePoint(10, 14, 13, 14, 65),
              SequencePoint(37, 14, 66, 14, 67),
              SequencePoint(38, 15, 17, 15, 93),
              SequencePoint(74, 16, 17, 16, 71),
              SequencePoint(102, 17, 13, 17, 14),
              SequencePoint(103, 18, 9, 18, 10),
              SequencePoint(104, 19, 5, 19, 6)
            )
          ),
          MethodDebugInformationData(Some(src),
                                     List(SequencePoint(0, 21, 31, 21, 32), SequencePoint(1, 21, 32, 21, 33))),
          MethodDebugInformationData(Some(src), List(SequencePoint(0, 6, 5, 6, 62)))
        )
      )

    }
  }
}
