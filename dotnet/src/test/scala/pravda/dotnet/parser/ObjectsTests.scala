package pravda.dotnet

package parser

import pravda.common.TestUtils
import pravda.dotnet.DotnetCompilation.dsl._
import pravda.dotnet.data.TablesData._
import utest._

object ObjectsTests extends TestSuite {

  val tests = Tests {
    'Object - {
      val Right(files) =
        steps(
          "Pravda.dll" -> Seq("PravdaDotNet/Pravda.cs"),
          "Objects.exe" -> Seq("Pravda.dll", "dotnet-tests/resources/Object.cs")
        ).run

      val pe = files.last.parsedPe

      TestUtils.assertEqual(
        pe.cilData.tables.typeDefTable,
        Vector(
          TypeDefData(0, 0, "<Module>", "", Ignored, Vector(), Vector()),
          TypeDefData(
            1,
            1048577,
            "A",
            "",
            TypeRefData(6, "Object", "System"),
            Vector(FieldData(0, 1, "AVal", 30)),
            Vector(MethodDefData(0, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "aVal"))),
                   MethodDefData(1, 0, 134, "AnswerA", 33, Vector()))
          ),
          TypeDefData(
            2,
            1048577,
            "B",
            "",
            TypeRefData(6, "Object", "System"),
            Vector(FieldData(1, 1, "BVal", 30)),
            Vector(MethodDefData(2, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "bVal"))),
                   MethodDefData(3, 0, 134, "AnswerB", 33, Vector()))
          ),
          TypeDefData(
            3,
            1048577,
            "Object",
            "",
            TypeRefData(6, "Object", "System"),
            Vector(),
            Vector(MethodDefData(4, 0, 134, "TestObjects", 33, Vector()),
                   MethodDefData(5, 0, 150, "Main", 37, Vector()),
                   MethodDefData(6, 0, 6278, ".ctor", 6, Vector()))
          )
        )
      )

      TestUtils.assertEqual(
        pe.cilData.tables.methodDefTable,
        Vector(
          MethodDefData(0, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "aVal"))),
          MethodDefData(1, 0, 134, "AnswerA", 33, Vector()),
          MethodDefData(2, 0, 6278, ".ctor", 1, Vector(ParamData(0, 1, "bVal"))),
          MethodDefData(3, 0, 134, "AnswerB", 33, Vector()),
          MethodDefData(4, 0, 134, "TestObjects", 33, Vector()),
          MethodDefData(5, 0, 150, "Main", 37, Vector()),
          MethodDefData(6, 0, 6278, ".ctor", 6, Vector())
        )
      )
    }
  }
}
