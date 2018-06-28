package pravda.cli.programs

import java.io.File
import java.nio.charset.StandardCharsets

import cats._
import cats.implicits._
import com.google.protobuf.ByteString
import pravda.cli.PravdaArgsParser
import pravda.cli.docs.GenDocsConfig
import pravda.cli.languages.IoLanguage

import scala.language.higherKinds

class GenDocs[F[_]: Monad](io: IoLanguage[F]) {

  def apply(config: GenDocsConfig): F[Unit] = {
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
