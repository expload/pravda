package pravda.testkit

import pravda.codegen.dotnet.DotnetCodegen
import pravda.common.TestUtils
import pravda.dotnet.DotnetCompilation.dsl._
import pravda.dotnet.parser.FileParser.ParsedDotnetFile
import pravda.dotnet.translation.Translator
import pravda.vm.asm.PravdaAssembler
import utest._

import scala.io.Source

object DotnetToCodegen extends TestSuite {

  val tests = Tests {
    'SmartProgram - {
      val Right(files) =
        steps(
          "Pravda.dll" -> Seq("PravdaDotNet/Pravda.cs"),
          "SmartProgram.exe" -> Seq("Pravda.dll", "dotnet-tests/resources/SmartProgram.cs")
        ).run

      val Right(asm) = Translator.translateAsm(List(ParsedDotnetFile(files.last.parsedPe, None)), None)
      val unityMethods = DotnetCodegen.generate(PravdaAssembler.assemble(asm, false)).head

      TestUtils.assertEqual(unityMethods,
                            ("SmartProgram.cs", Source.fromResource("SmartProgram.generated.cs").mkString))
    }

    'ZooProgram - {
      val Right(files) =
        steps(
          "Pravda.dll" -> Seq("PravdaDotNet/Pravda.cs"),
          "ZooProgram.exe" -> Seq("Pravda.dll", "dotnet-tests/resources/ZooProgram.cs")
        ).run

      val Right(asm) = Translator.translateAsm(List(ParsedDotnetFile(files.last.parsedPe, None)), None)
      val unityMethods = DotnetCodegen.generate(PravdaAssembler.assemble(asm, false)).head
      TestUtils.assertEqual(unityMethods, ("ZooProgram.cs", Source.fromResource("ZooProgram.generated.cs").mkString))
    }
  }
}
