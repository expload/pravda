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
import pravda.cli.languages.{IoLanguage, VmLanguage}
import pravda.common.bytes
import pravda.node.data.blockchain.ExecutionInfo
import pravda.vm.ExecutionResult
import pravda.node.data.serialization._
import pravda.node.data.serialization.json._
import scala.language.higherKinds

class RunBytecode[F[_]: Monad](io: IoLanguage[F], vm: VmLanguage[F]) {

  def apply(config: PravdaConfig.RunBytecode): F[Unit] = {
    val errorOrMemory: EitherT[F, String, ExecutionResult] =
      for {
        storagePath <- EitherT.liftF(config.storage.fold(io.createTmpDir())(path => Monad[F].pure(path)))
        executor = bytes.hex2byteString(config.executor)
        program <- useOption(config.input)(io.readFromStdin(),
                                           path => io.readFromFile(path).map(_.toRight(s"`$path` is not found.\n")))
        memory <- EitherT(vm.run(program, executor, storagePath, Long.MaxValue))
      } yield {
        memory
      }
    errorOrMemory.value flatMap {
      case Left(error) => io.writeStringToStderrAndExit(error)
      case Right(memory) =>
        val str = transcode(ExecutionInfo.from(memory)).to[Json]
        io.writeStringToStdout(str)
    }
  }
}
