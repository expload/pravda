package pravda.cli.languages

import com.google.protobuf.ByteString

import scala.language.higherKinds

trait IoLanguage[F[_]] {
  def createTmpDir(): F[String]
  def readFromStdin(): F[ByteString]
  def readFromFile(path: String): F[Option[ByteString]]
  def writeToStdout(data: ByteString): F[Unit]
  def writeStringToStdout(data: String): F[Unit]
  def writeStringToStderrAndExit(data: String, code: Int = 1): F[Unit]
  def writeToFile(path: String, data: ByteString): F[Unit]
  def exit(code: Int): F[Unit]
}
