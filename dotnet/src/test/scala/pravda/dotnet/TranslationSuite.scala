package pravda.dotnet

object TranslationSuiteData {
  final case class Input(exe: String)
  final case class Output(translation: String)
}

import java.io.File

import pravda.plaintest.Plaintest
import TranslationSuiteData._
import pravda.dotnet.translation.Translator
import pravda.vm.asm.PravdaAssembler

object TranslationSuite extends Plaintest[Input, Output] {

  lazy val dir = new File("dotnet/src/test/resources/translation")
  override lazy val ext = "trs"
  override lazy val allowOverwrite = true

  def produce(input: Input): Either[String, Output] = {
    val parts = input.exe.split("\\s+").toList
    val exe = parts.head
    val pdbE = if (parts.length > 1) {
      parsePdbFile(parts(1)).map(p => Some(p._2))
    } else {
      Right(None)
    }

    for {
      pe <- parsePeFile(exe)
      pdb <- pdbE
      (_, cilData, methods, signatures) = pe
      asm <- Translator.translateAsm(methods, cilData, signatures, pdb).left.map(_.mkString)
    } yield Output(PravdaAssembler.render(asm))
  }
}
