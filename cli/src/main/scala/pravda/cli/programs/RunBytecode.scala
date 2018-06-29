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
