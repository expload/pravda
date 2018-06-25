package pravda.cli.programs

import java.io.File
import java.nio.charset.StandardCharsets

import cats._
import cats.implicits._
import com.google.protobuf.ByteString
import pravda.cli.docs.GenDocsConfig
import pravda.cli.languages.IoLanguage
import pravda.cli.{PravdaArgsParser, PravdaConfig}
import pravda.cmdopt.printers.MarkdownPrinter

import scala.language.higherKinds

class GenDocs[F[_]: Monad](io: IoLanguage[F]) {

  private val printer = new MarkdownPrinter[PravdaConfig]

  def apply(config: GenDocsConfig): F[Unit] = {
    val mainPage = (
      new File(config.outDir, config.mainPageName),
      ByteString.copyFrom(printer.printVerbs(PravdaArgsParser.model), StandardCharsets.UTF_8)
    )
    val pages = PravdaArgsParser.paths.map { path =>
      val name = new File(config.outDir, s"${path.map(_.name).mkString("-")}.md")
      val content = printer.printVerbs(path.reverse.head.verbs)
      val bcontent = ByteString.copyFrom(content, StandardCharsets.UTF_8)
      (name, bcontent)
    } :+ mainPage
    for {
      _ <- io.mkdirs(config.outDir)
      _ <- pages.map {
        case (name, content) =>
          io.writeToFile(name.getAbsolutePath, content)
      }.sequence
    } yield ()
  }
}
