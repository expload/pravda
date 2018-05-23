package pravda.cli.programs

import cats._
import cats.implicits._
import cats.data.EitherT
import com.google.protobuf.ByteString
import pravda.cli.Config
import pravda.cli.Config.PravdaCompile
import pravda.cli.languages.{CompilersLanguage, IoLanguage}

import scala.language.higherKinds

class Compile[F[_]: Monad](io: IoLanguage[F], compilers: CompilersLanguage[F]) {

  import PravdaCompile._

  def apply(config: Config.Compile): F[Unit] = {
    val errorOrFinished =
      for {
        input <- EitherT[F, String, ByteString] {
          config.input.fold[F[Either[String, ByteString]]](io.readFromStdin().map(Right.apply)) { path =>
            io.readFromFile(path).map(_.toRight(s"`$path` is not found."))
          }
        }
        result <- EitherT[F, String, ByteString] {
          config.compiler match {
            case Asm    => compilers.asm(input.toStringUtf8).map(x => Right(x))
            case Disasm => compilers.disasm(input).map(s => Right(ByteString.copyFromUtf8(s)))
            case Forth  => compilers.forth(input.toStringUtf8).map(s => Right(s))
            case DotNet => Monad[F].pure(Left(".NET currently is not supported."))
            case Nope   => Monad[F].pure(Left("Compilation mode should be selected."))
          }
        }
        _ <- EitherT.liftF[F, String, Unit](config.output.fold(io.writeToStdout(result))(io.writeToFile(_, result)))
      } yield ()

    errorOrFinished.value.flatMap {
      case Left(error) => io.writeStringToStderrAndExit(s"$error\n")
      case Right(_)    => io.exit(0)
    }
  }
}
