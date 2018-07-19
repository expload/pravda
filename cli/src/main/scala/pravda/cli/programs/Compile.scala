/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.cli.programs

import cats._
import cats.data.EitherT
import cats.implicits._
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
            case Asm =>
              config.input match {
                case Some(fileName) => compilers.asm(fileName, input.toStringUtf8)
                case None           => compilers.asm(input.toStringUtf8)
              }
            case Disasm => compilers.disasm(input).map(s => Right(ByteString.copyFromUtf8(s)))
            case DotNet => compilers.dotnet(input)
            case DotNetVisualize =>
              for {
                dv <- compilers.dotnetVisualize(input)
                code <- dv.map {
                  case (code, visualization) => io.writeToStdout(ByteString.copyFromUtf8(visualization)).map(_ => code)
                }.sequence
              } yield code
            case Nope => Monad[F].pure(Left("Compilation mode should be selected."))
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
