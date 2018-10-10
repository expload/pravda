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
        inputs <- EitherT[F, String, List[(String, ByteString)]] {
          config.input match {
            case Nil => io.readFromStdin().map(s => Right(List("stdin" -> s)))
            case nonEmpty =>
              for {
                paths <- nonEmpty
                  .map(path =>
                    for {
                      isf <- io.isFile(path).map(_.getOrElse(false))
                      paths <- if (isf) Monad[F].pure(List(path)) else io.listFiles(path)
                    } yield paths)
                  .flatSequence
                inputs <- paths
                  .map(path =>
                    for {
                      file <- io.readFromFile(path)
                    } yield file.map(f => path -> f).toRight(s"`$path` is not found."))
                  .sequence
              } yield inputs.sequence
          }
        }
        result <- EitherT[F, String, ByteString] {
          config.compiler match {
            case Asm =>
              inputs match {
                case List(("stdin", f)) => compilers.asm(f.toStringUtf8)
                case List((path, f))    => compilers.asm(path, f.toStringUtf8)
                case _                  => Monad[F].pure(Left("Asm compilation takes only one file."))
              }
            case Disasm =>
              inputs match {
                case List((path, f)) => compilers.disasm(f).map(s => Right(ByteString.copyFromUtf8(s)))
                case _               => Monad[F].pure(Left("Disassembly takes only one file."))
              }

            case DotNet =>
              inputs.find {
                case (p, f) => !p.endsWith(".exe") && !p.endsWith(".dll") && !p.endsWith(".pdb") && p != "stdin"
              } match {
                case Some((p, f)) => Monad[F].pure(Left(s"Wrong file extension: $p"))
                case None =>
                  val dotnetFilesE: Either[String, List[(ByteString, Option[ByteString])]] =
                    inputs
                      .groupBy { case (p, _) => p.dropRight(4) }
                      .map {
                        case (prefix, files) =>
                          val exeO = files.find { case (path, _) => path == s"$prefix.exe" || path == "stdin" }
                          val dllO = files.find { case (path, _) => path == s"$prefix.dll" }
                          val pdbO = files.find { case (path, _) => path == s"$prefix.pdb" }

                          (exeO, dllO) match {
                            case (Some((exePath, _)), Some((dllPath, _))) =>
                              Left(s".dll and .exe files have the same name: $exePath, $dllPath")
                            case (None, None) =>
                              Left(s".dll or .exe is not specified: $prefix")
                            case (Some((exePath, exeContent)), None) =>
                              Right((exeContent, pdbO.map(_._2)))
                            case (None, Some((dllPath, dllContnet))) =>
                              Right((dllContnet, pdbO.map(_._2)))
                          }
                      }
                      .toList
                      .sequence

                  dotnetFilesE match {
                    case Left(err)          => Monad[F].pure(Left(err))
                    case Right(dotnetFiles) => compilers.dotnet(dotnetFiles, config.mainClass)
                  }
              }
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
