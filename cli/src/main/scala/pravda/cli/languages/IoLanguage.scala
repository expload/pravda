package pravda.cli.languages

import com.google.protobuf.ByteString

import scala.language.higherKinds

trait IoLanguage[F[_]] {
  def createTmpDir(): F[String]
  def readFromStdin(): F[ByteString]
  // TODO maybe here we should have either instead of option
  def readFromFile(path: String): F[Option[ByteString]]
  def writeToStdout(data: ByteString): F[Unit]
  def writeStringToStdout(data: String): F[Unit]
  def writeStringToStderrAndExit(data: String, code: Int = 1): F[Unit]
  // TODO maybe here we should have either instead of unit
  def writeToFile(path: String, data: ByteString): F[Unit]
  def exit(code: Int): F[Unit]
}
