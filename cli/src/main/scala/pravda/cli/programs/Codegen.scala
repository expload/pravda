package pravda.cli.programs

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import pravda.cli.PravdaConfig
import pravda.cli.PravdaConfig.CodegenMode.Dotnet
import pravda.cli.languages.{CodeGeneratorsLanguage, IoLanguage}

import scala.language.higherKinds

class Codegen[F[_]: Monad](io: IoLanguage[F], codegen: CodeGeneratorsLanguage[F]) {

  def apply(config: PravdaConfig.Codegen): F[Unit] = {
    val errorOrResult: EitherT[F, String, List[(String, String)]] =
      for {
        input <- useOption(config.input)(
          io.readFromStdin(),
          path => io.readFromFile(path).map(_.toRight(s"`$path` is not found."))
        )
        result <- EitherT[F, String, List[(String, String)]] {
          config.codegenMode match {
            case Dotnet => codegen.dotnet(input, config.excludeBigInteger).map(Right(_))
          }
        }
      } yield {
        result
      }

    val dir = config.outDir.getOrElse("codegen")

    errorOrResult.value.flatMap {
      case Left(error) => io.writeStringToStderrAndExit(s"$error\n")
      case Right(result) =>
        result
          .map {
            case (filename, content) =>
              for {
                _ <- io.mkdirs(dir)
                path <- io.concatPath(dir, filename)
                _ <- io.writeStringToStdout(s"Writing to $path\n")
                _ <- io.writeToFile(path, content)
              } yield ()
          }
          .sequence
          .map(_ => ())
    }
  }
}
