package pravda.cli.programs

import cats._
import cats.implicits._
import cats.data.EitherT
import com.google.protobuf.ByteString
import pravda.cli.PravdaConfig
import pravda.cli.PravdaConfig.CompileMode
import pravda.cli.languages.{CompilersLanguage, IoLanguage}

import scala.language.higherKinds

class Compile[F[_]: Monad](io: IoLanguage[F], compilers: CompilersLanguage[F]) {

  import CompileMode._

  def apply(config: PravdaConfig.Compile): F[Unit] = {
    val errorOrResult: EitherT[F, String, ByteString] =
      for {
        input <- useOption(config.input)(
          io.readFromStdin(),
          path => io.readFromFile(path).map(_.toRight(s"`$path` is not found."))
        )
        result <- EitherT[F, String, ByteString] {
          config.compiler match {
            case Asm    => compilers.asm(input.toStringUtf8)
            case Disasm => compilers.disasm(input).map(s => Right(ByteString.copyFromUtf8(s)))
            case DotNet => compilers.dotnet(input)
            case Nope   => Monad[F].pure(Left("Compilation mode should be selected."))
          }
        }
      } yield {
        result
      }

    errorOrResult.value.flatMap {
      case Left(error)   => io.writeStringToStderrAndExit(s"$error\n")
      case Right(result) => config.output.fold(io.writeToStdout(result))(io.writeToFile(_, result))
    }
  }
}
