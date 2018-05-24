package pravda.cli.languages

import com.google.protobuf.ByteString
import pravda.vm.state.Memory

import scala.language.higherKinds

trait VmLanguage[F[_]] {
  def run(program: ByteString, executor: ByteString, storagePath: String): F[Either[String, Memory]]
}
