package pravda.dotnet

package parser

import pravda.common.TestUtils
import pravda.dotnet.data.TablesData._
import utest._

object InheritanceTests extends TestSuite {

  val tests = Tests {
    'inheritanceParse - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("inheritance.exe")

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
