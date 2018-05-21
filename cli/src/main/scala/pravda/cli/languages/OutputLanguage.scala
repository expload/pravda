package pravda.cli.languages

import com.google.protobuf.ByteString

import scala.language.higherKinds

trait OutputLanguage[F[_]] {
  def writeToStdout(data: ByteString): F[Unit]
  def saveToFile(path: String, data: ByteString): F[Unit]
}
