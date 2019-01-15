package pravda.dotnet

import java.io.File

import pravda.plaintest._

object ParserSuiteData {
  final case class Input(`dotnet-compilation`: DotnetCompilation)
  final case class Output(methods: String, signatures: String)
}

import ParserSuiteData._

object ParserSuite extends Plaintest[Input, Output] {

  lazy val dir = new File("dotnet/src/test/resources/parser")
  override lazy val ext = "prs"
  override lazy val allowOverwrite = true

  def produce(input: Input): Either[String, Output] =
    for {
      files <- DotnetCompilation.run(input.`dotnet-compilation`)
      clearedFiles = clearPathsInPdb(files)
      last = clearedFiles.last
    } yield
      Output(
        pprint.apply(last.parsedPe.methods, height = Int.MaxValue).plainText,
        pprint.apply(last.parsedPe.signatures.toList.sortBy(_._1), height = Int.MaxValue).plainText
      )
}
