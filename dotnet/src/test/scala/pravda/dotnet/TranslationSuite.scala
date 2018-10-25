package pravda.dotnet

import java.io.File

import pravda.dotnet.TranslationSuiteData._
import pravda.dotnet.translation.Translator
import pravda.plaintest.Plaintest
import pravda.vm.asm.PravdaAssembler

object TranslationSuiteData {
  final case class Input(`dotnet-compilation`: DotnetCompilation)
  final case class Output(translation: String)
}

object TranslationSuite extends Plaintest[Input, Output] {

  lazy val dir = new File("dotnet/src/test/resources/translation")
  override lazy val ext = "trs"
  override lazy val allowOverwrite = true

  def produce(input: Input): Either[String, Output] = {
    for {
      files <- DotnetCompilation.run(input.`dotnet-compilation`)
      asm <- Translator.translateAsm(files, input.`dotnet-compilation`.`main-class`).left.map(_.mkString)
    } yield Output(PravdaAssembler.render(asm))
  }
}
