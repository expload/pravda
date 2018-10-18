package pravda.dotnet

import java.io.File

import pravda.plaintest.Plaintest
import TranslationSuiteData._
import pravda.dotnet.translation.Translator
import pravda.vm.asm.PravdaAssembler
import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import pravda.dotnet.parser.FileParser.ParsedDotnetFile

object TranslationSuiteData {
  final case class Input(exe: String)
  final case class Output(translation: String)
}

object TranslationSuite extends Plaintest[Input, Output] {

  lazy val dir = new File("dotnet/src/test/resources/translation")
  override lazy val ext = "trs"
  override lazy val allowOverwrite = true

  def produce(input: Input): Either[String, Output] = {
    val lines = input.exe.lines.toList
    val parts = lines.head.split("\\s+").toList
    val mainClass = lines.tail.headOption
    val filesE: Either[String, List[ParsedDotnetFile]] = parts
      .groupBy(_.dropRight(4))
      .map {
        case (prefix, files) =>
          val exeO = files.find(_ == s"$prefix.exe")
          val dllO = files.find(_ == s"$prefix.dll")
          val pdbO = files.find(_ == s"$prefix.pdb")

          (exeO, dllO) match {
            case (Some(exe), Some(dll)) =>
              Left(s".dll and .exe files have the same name: $exe, $dll")
            case (None, None) =>
              Left(s".dll or .exe is not specified: $prefix")
            case (Some(exe), None) =>
              parseDotnetFile(exe, pdbO)
            case (None, Some(dll)) =>
              parseDotnetFile(dll, pdbO)
          }
      }
      .toList
      .sequence

    for {
      files <- filesE
      asm <- Translator.translateAsm(files, mainClass).left.map(_.mkString)
    } yield Output(PravdaAssembler.render(asm))
  }
}
