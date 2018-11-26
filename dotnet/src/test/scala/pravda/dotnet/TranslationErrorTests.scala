package pravda.dotnet

import pravda.dotnet.DotnetCompilation.dsl._
import pravda.dotnet.translation.Translator
import utest._

object TranslationErrorTests extends TestSuite {

  val tests = Tests {
    'Error - {
      val Right(files) =
        steps(
          "Pravda.dll" -> Seq("PravdaDotNet/Pravda.cs"),
          "Error.exe" -> Seq("Pravda.dll", "dotnet-tests/resources/Error.cs")
        ).run

      val pe = files.last.parsedPe
      val pdb = files.last.parsedPdb.get

      Translator.translateAsm(pe, Some(pdb)).left.get.mkString ==>
        """|Call(MemberRefData(TypeRefData(6,Console,System),WriteLine,16)) is not supported
           |  Error.cs:9,9-9,74""".stripMargin
    }

    'PublicMapping - {
      val Right(files) =
        steps(
          "Pravda.dll" -> Seq("PravdaDotNet/Pravda.cs"),
          "PublicMapping.exe" -> Seq("Pravda.dll", "dotnet-tests/resources/PublicMapping.cs")
        ).run

      val pe = files.last.parsedPe
      val pdb = files.last.parsedPdb.get

      Translator.translateAsm(pe, Some(pdb)).left.get.mkString ==>
        "All Mapping must be private: M in PublicMapping is not private"
    }
  }
}
