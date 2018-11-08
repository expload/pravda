package pravda.dotnet.parser

import pravda.common.TestUtils
import pravda.dotnet.DotnetCompilation.dsl.steps
import pravda.dotnet.DotnetCompilation.dsl._
import pravda.dotnet.data.TablesData._
import utest._

object ProgramInterfaceTests extends TestSuite {

  val tests = Tests {
    'ProgramInterface - {
      val Right(files) =
        steps(
          "Pravda.dll" -> Seq("PravdaDotNet/Pravda.cs"),
          "ProgramInterface.dll" -> Seq("Pravda.dll", "dotnet-tests/resources/ProgramInterface.cs")
        ).run

      val pe = files.last.parsedPe

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
              161,
              "ProgramInterface",
              "InterfaceNamespace",
              Ignored,
              Vector(),
              Vector(MethodDefData(0, 0, 1478, "Add", 29, Vector(ParamData(0, 1, "a"), ParamData(0, 2, "b"))))),
            MemberRefData(TypeRefData(10, "Program", "Expload.Pravda"), ".ctor", 6)
          ),
          CustomAttributeData(
            TypeDefData(
              2,
              1048577,
              "ProgramInterfaceImpl",
              "InterfaceNamespace",
              TypeRefData(6, "Object", "System"),
              Vector(),
              Vector(
                MethodDefData(1, 0, 486, "Add", 29, Vector(ParamData(0, 1, "a"), ParamData(0, 2, "b"))),
                MethodDefData(2, 0, 150, "Main", 35, Vector()),
                MethodDefData(3, 0, 6278, ".ctor", 6, Vector())
              )
            ),
            MemberRefData(TypeRefData(10, "Program", "Expload.Pravda"), ".ctor", 6)
          )
        )
      )
    }
  }
}
