package pravda.dotnet
package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object ObjectsTests extends TestSuite {

  val tests = Tests {
    'objectsTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("objects.exe")

      println(Translator.translateAsm(methods, cilData, signatures).left.map(_.mkString))

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse("""
        |
      """.stripMargin).right.get
      )
    }
  }
}
