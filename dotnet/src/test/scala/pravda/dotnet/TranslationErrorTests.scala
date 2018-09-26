package pravda.dotnet

import pravda.dotnet.translation.Translator
import utest._

object TranslationErrorTests extends TestSuite {

  val tests = Tests {
    'error - {
      val Right(pe) = parsePeFile("error.exe")
      val Right(pdb) = parsePdbFile("error.pdb")

      Translator.translateAsm(pe, Some(pdb)).left.get.mkString ==>
        """|Call(MemberRefData(TypeRefData(6,Console,System),WriteLine,16)) is not supported
           |  error.cs:8,9-8,74""".stripMargin
    }
  }
}
