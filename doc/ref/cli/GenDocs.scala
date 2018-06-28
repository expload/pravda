package pravda.cli.docs

import java.io.File
import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import cats.Monad
import cats.implicits._
import com.google.protobuf.ByteString
import pravda.cli.PravdaArgsParser
import pravda.cli.languages.IoLanguage
import pravda.cli.languages.impl.IoLanguageImpl
import pravda.yopt.CommandLine
import pravda.yopt.CommandLine.{HelpNeeded, Ok, ParseError}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.language.higherKinds
import scala.sys.process.stderr

object GenDocs extends App {

  final case class GenDocsConfig(outDir: String = "docs", mainPageName: String = "main.md")

  object GenDocsArgsParser extends CommandLine[GenDocsConfig] {

    val model =
      head("gen-docs")
        .text("Generate documentation for Pravda Command line tool.")
        .children(
          opt[File]('o', "output")
            .text("Output directory")
            .action {
              case (file, GenDocsConfig(outDir, mainPageName)) =>
                GenDocsConfig(file.getAbsolutePath, mainPageName)
              case (_, otherwise) => otherwise
            }
        )
  }

  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  lazy val io = new IoLanguageImpl()
  lazy val genDocs = new GenDocs(io)

  val eventuallyExitCode = GenDocsArgsParser.parse(args.toList, GenDocsConfig()) match {
    case Ok(config: GenDocsConfig) => genDocs(config).map(_ => 0)
    case HelpNeeded(cli) =>
      Future {
        print(GenDocsArgsParser.root.toHelpString)
        0
      }
    case ParseError(msg) =>
      Future {
        stderr.println(msg)
        stderr.print(GenDocsArgsParser.root.toHelpString)
        1 // every non zero exit code says about error
      }
  }

  val exitCode = Await.result(
    awaitable = for (exitCode <- eventuallyExitCode; _ <- system.terminate()) yield exitCode,
    atMost = 5.minutes
  )

  sys.exit(exitCode)
}

class GenDocs[F[_]: Monad](io: IoLanguage[F]) {

  def apply(config: GenDocs.GenDocsConfig): F[Unit] = {
    val mainPage = (
      new File(config.outDir, config.mainPageName),
      ByteString.copyFrom(PravdaArgsParser.root.toMarkdown, StandardCharsets.UTF_8)
    )
    val pages = PravdaArgsParser.paths.map { path =>
      val name = new File(config.outDir, s"${path.toString}.md")
      val content = path.toMarkdown
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

