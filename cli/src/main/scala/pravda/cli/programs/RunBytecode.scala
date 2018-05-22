package pravda.cli.programs

import cats._
import cats.implicits._
import com.google.protobuf.ByteString
import pravda.cli.Config
import pravda.cli.languages.{IoLanguage, VmLanguage}
import pravda.common.bytes
import pravda.vm.state.Memory

import scala.language.higherKinds

class RunBytecode[F[_]: Monad](io: IoLanguage[F], vm: VmLanguage[F]) {

  def memoryToJson(memory: Memory): String = {
    val stack = memory.stack.map(x => '"' + bytes.byteString2hex(x) + '"')
    val heap = memory.heap.map(x => '"' + bytes.byteString2hex(x) + '"')
    s"""{"stack":[${stack.mkString(",")}],"heap":[${heap.mkString(",")}]}"""
  }

  def apply(program: ByteString, executor: ByteString, storagePath: String): F[Unit] =
    for {
      memory <- vm.run(program, executor, storagePath)
      json = memoryToJson(memory)
      _ <- io.writeStringToStdout(json)
      _ <- io.exit(0)
    } yield ()

  def apply(config: Config.RunBytecode): F[Unit] =
    for {
      storagePath <- config.storage.fold(io.createTmpDir())(path => Monad[F].pure(path))
      executor = bytes.hex2byteString(config.executor)
      _ <- config.input match {
        case None => io.readFromStdin().flatMap(apply(_, executor, storagePath))
        case Some(path) =>
          io.readFromFile(path).flatMap {
            case None          => io.writeStringToStderrAndExit(s"`$path` is not found.\n")
            case Some(program) => apply(program, executor, storagePath)
          }
      }
    } yield ()
}
