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

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import pravda.cli.PravdaConfig
import pravda.cli.PravdaConfig.CodegenMode.Dotnet
import pravda.codegen.dotnet.DotnetCodegen
import pravda.node.client.{CompilersLanguage, IoLanguage, IpfsLanguage, MetadataLanguage}

import scala.language.higherKinds

class Codegen[F[_]: Monad](io: IoLanguage[F],
                           compilers: CompilersLanguage[F],
                           ipfs: IpfsLanguage[F],
                           metadata: MetadataLanguage[F]) {

  def apply(config: PravdaConfig.Codegen): F[Unit] = {
    val errorOrResult: EitherT[F, String, List[(String, String)]] =
      for {
        input <- useOption(config.input)(
          io.readFromStdin(),
          path => io.readFromFile(path).map(_.toRight(s"`$path` is not found."))
        )
        result <- EitherT[F, String, List[(String, String)]] {
          config.codegenMode match {
            case Dotnet =>
              for {
                loaded <- if (config.metaFromIpfs) {
                  MetaOps.loadAllMeta(input, config.ipfsNode)(compilers, ipfs, metadata)
                } else {
                  MetaOps.loadMetaFromSource(input)(compilers)
                }
              } yield Right(DotnetCodegen.generate(loaded).toList)
          }
        }
      } yield result

    errorOrResult.value.flatMap {
      case Left(error) => io.writeStringToStderrAndExit(s"$error\n")
      case Right(result) =>
        result
          .map {
            case (filename, content) =>
              for {
                _ <- io.mkdirs("Assets")
                path <- io.concatPath("Assets", filename)
                _ <- io.writeStringToStdout(s"Writing to $path\n")
                _ <- io.writeToFile(path, content)
              } yield ()
          }
          .sequence
          .map(_ => ())
    }
  }
}
