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
import pravda.cli.PravdaConfig
import pravda.common.bytes
import pravda.node.client._
import pravda.common.serialization._
import pravda.node.data.serialization.json._
import pravda.vm.asm.SourceMap

import scala.language.higherKinds

class RunBytecode[F[_]: Monad](io: IoLanguage[F],
                               vm: VmLanguage[F],
                               compilers: CompilersLanguage[F],
                               ipfs: IpfsLanguage[F],
                               metadata: MetadataLanguage[F]) {

  def apply(config: PravdaConfig.RunBytecode): F[Unit] = {
    val errorOrMemory: EitherT[F, String, String] =
      for {
        appStateDbPath <- EitherT.liftF(
          config.appStateDbPath.fold(io.createTmpDir().map(_ + "application-state"))(path => Monad[F].pure(path)))
        effectsDbPath <- EitherT.liftF(
          config.effectsDbPath.fold(io.createTmpDir().map(_ + "effects"))(path => Monad[F].pure(path)))
        executor = bytes.hex2byteString(config.executor)
        program <- useOption(config.input)(io.readFromStdin(),
                                           path => io.readFromFile(path).map(_.toRight(s"`$path` is not found.\n")))
        loaded <- if (config.metaFromIpfs) {
          EitherT.right(MetaOps.loadAllMeta(program, config.ipfsNode)(compilers, ipfs, metadata))
        } else {
          EitherT.right(MetaOps.loadMetaFromSource(program)(compilers))
        }
        memory <- EitherT(vm.run(program, executor, appStateDbPath, effectsDbPath, Long.MaxValue)).leftMap { re =>
          val st = SourceMap.renderStackTrace(SourceMap.stackTrace(loaded, re), 2)
          s"${transcode(re.finalState).to[Json]}\n${re.error}\n$st\n"
        }
      } yield {
        transcode(memory).to[Json] + "\n"
      }
    errorOrMemory.value flatMap {
      case Left(out)  => io.writeStringToStderrAndExit(out)
      case Right(out) => io.writeStringToStdout(out)
    }
  }
}
