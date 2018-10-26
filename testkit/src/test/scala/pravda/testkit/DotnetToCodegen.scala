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
    'smart_program - {
      val Right(files) =
        steps(
          "Pravda.dll" -> Seq("PravdaDotNet/Pravda.cs"),
          "SmartProgram.exe" -> Seq("Pravda.dll", "dotnet-tests/resources/SmartProgram.cs")
        ).run

      val Right(asm) = Translator.translateAsm(List(ParsedDotnetFile(files.last.parsedPe, None)), None)
      val unityMethods = DotnetCodegen.generate(PravdaAssembler.assemble(asm, false))

      TestUtils.assertEqual(unityMethods, ("Program.cs", Source.fromResource("smart_program.generated.cs").mkString))
    }

    'zoo_program - {
      val Right(pe) =
        FileParser.parsePe(Files.readAllBytes(new File(getClass.getResource("/zoo_program.exe").getPath).toPath))
      val Right(asm) = Translator.translateAsm(List(ParsedDotnetFile(pe, None)), None)
      val unityMethods = DotnetCodegen.generate(PravdaAssembler.assemble(asm, false))

      TestUtils.assertEqual(unityMethods, ("Program.cs", Source.fromResource("zoo_program.generated.cs").mkString))
    }
  }
}
