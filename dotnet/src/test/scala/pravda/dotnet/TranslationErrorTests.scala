package pravda.dotnet

import pravda.dotnet.translation.Translator
import utest._

object TranslationErrorTests extends TestSuite {

  val tests = Tests {
    'error - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("error.exe")
      val Right((_, pdbTables)) = parsePdbFile("error.pdb")

      Translator.translateAsm(methods, cilData, signatures, Some(pdbTables)).left.get.mkString ==>
        """|Call(MemberRefData(TypeRefData(6,Console,System),WriteLine,16)) is not supported
           |  error.cs:8,9-8,74""".stripMargin
    }
  }
}
