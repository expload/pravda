package pravda.cli.programs

import cats._
import cats.implicits._
import pravda.cli.Config
import pravda.cli.ArgumentsParser
import pravda.cli.languages.{IoLanguage}

import java.io.File
import java.nio.charset.StandardCharsets
import com.google.protobuf.ByteString

import scala.language.higherKinds

class GenDocs[F[_]: Monad](io: IoLanguage[F]) {

  import pravda.cmdopt.instances.show.markdown._

  def apply(config: Config.GenDocs): F[Unit] = {
    val mainPage = (
      new File(config.outDir, config.mainPageName),
      ByteString.copyFrom(fshow(config.cl.model), StandardCharsets.UTF_8)
    )
    val pages = ArgumentsParser.paths.map { path =>
      val name = new File(config.outDir, s"${path.map(_.name).mkString("-")}.md")
      val content = fshow(path.reverse.head.verbs)
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
