package pravda.dotnet

package translation

import utest._

object ErrorTests extends TestSuite {

  val tests = Tests {
    'helloWorld - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("error.exe")
      val Right((_, pdbTables)) = parsePdbFile("error.pdb")

      Translator.translateAsm(methods, cilData, signatures, Some(pdbTables)).left.get.mkString ==>
        """|Call(MemberRefData(TypeRefData(6,Console,System),WriteLine,16)) is not supported
         |  error.cs:8,9-8,74""".stripMargin
    }
  }
}
