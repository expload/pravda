package pravda.testkit

import pravda.dotnet.parsers.FileParser
import pravda.dotnet.translation.Translator
import pravda.vm.asm.PravdaAssembler
import utest._

object SmartProgramSandbox extends TestSuite {

  val tests = Tests {
    val smartProgram = {
      val Right((_, cilData, methods, signatures)) = FileParser.parseFile("smart_program.exe")
      val Right(asm) = Translator.translateAsm(methods, cilData, signatures)
      PravdaAssembler.render(asm)
    }

    'balanceOf - {
      val c = VmSandbox.parseCase(
        "balanceOf",
        s"""
        |preconditions:
        |  watts-limit: 10000
        |  stack:
        |    x0011, "balanceOf"
        |  heap:
        |  storage:
        |    x62616C616E6365730011 = int32(10)
        |
        |expectations:
        |  watts-spent: 264
        |  stack:
        |    int32(10)
        |  heap:
        |  effects:
        |   sget x62616C616E6365730011 int32(10),
        |   sget x62616C616E6365730011 int32(10)
        |---
        |$smartProgram
      """.stripMargin
      )

      VmSandbox.assertCase(c.right.get)
    }
  }
}
