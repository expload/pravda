package pravda.testkit

import pravda.codegen.dotnet.DotnetCodegen
import pravda.dotnet.parsers.FileParser
import pravda.dotnet.translation.Translator
import pravda.vm.asm.PravdaAssembler
import utest._

import scala.io.Source

object DotnetToCodegen extends TestSuite {

  val tests = Tests {
    'smart_program - {
      val Right((_, cilData, methods, signatures)) = FileParser.parseFile("smart_program.exe")
      val Right(asm) = Translator.translateAsm(methods, cilData, signatures)
      val (_, unityMethods) = DotnetCodegen.generate(PravdaAssembler.assemble(asm, false))

      unityMethods ==> (("Program.cs", Source.fromResource("smart_program.generated.cs").mkString))
    }
  }
}
