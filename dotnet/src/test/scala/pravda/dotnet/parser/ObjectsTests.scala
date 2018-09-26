package pravda.dotnet

package parser

import pravda.common.TestUtils
import pravda.dotnet.data.TablesData._
import utest._

object ObjectsTests extends TestSuite {

  val tests = Tests {
    'objectsParse - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("objects.exe")

      TestUtils.assertEqual(
        cilData.tables.typeDefTable,
        Vector(
          TypeDefData(0, 0, "<Module>", "", Ignored, Vector(), Vector()),
          TypeDefData(
            1,
            1048577,
            "A",
            "",
            TypeRefData(6, "Object", "System"),
            Vector(FieldData(1, "AVal", 38)),
            Vector(MethodDefData(0, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "aVal"))),
                   MethodDefData(1, 0, 134, "AnswerA", 41, Vector()))
          ),
          TypeDefData(
            2,
            1048577,
            "B",
            "",
            TypeRefData(6, "Object", "System"),
            Vector(FieldData(1, "BVal", 38)),
            Vector(MethodDefData(2, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "bVal"))),
                   MethodDefData(3, 0, 134, "AnswerB", 41, Vector()))
          ),
          TypeDefData(
            3,
            1048577,
            "MyProgram",
            "",
            TypeRefData(6, "Object", "System"),
            Vector(),
            Vector(MethodDefData(4, 0, 134, "Func", 41, Vector()),
                   MethodDefData(5, 0, 150, "Main", 45, Vector()),
                   MethodDefData(6, 0, 6278, ".ctor", 6, Vector()))
          )
        )
      )

      TestUtils.assertEqual(
        cilData.tables.methodDefTable,
        Vector(
          MethodDefData(0, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "aVal"))),
          MethodDefData(1, 0, 134, "AnswerA", 41, Vector()),
          MethodDefData(2, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "bVal"))),
          MethodDefData(3, 0, 134, "AnswerB", 41, Vector()),
          MethodDefData(4, 0, 134, "Func", 41, Vector()),
          MethodDefData(5, 0, 150, "Main", 45, Vector()),
          MethodDefData(6, 0, 6278, ".ctor", 6, Vector())
        )
      )
    }
  }
}
