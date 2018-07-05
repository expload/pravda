package pravda.cli.languages

import com.google.protobuf.ByteString

import scala.language.higherKinds

trait IoLanguage[F[_]] {
  def mkdirs(path: String): F[Unit]
  def pwd(): F[String]
  def isDirectory(path: String): F[Option[Boolean]]
  def createTmpDir(): F[String]
  def readFromStdin(): F[ByteString]
  def concatPath(parent: String, child: String): F[String]
  // TODO maybe here we should have either instead of option
  def readFromFile(path: String): F[Option[ByteString]]
  def writeToStdout(data: ByteString): F[Unit]
  def writeStringToStdout(data: String): F[Unit]
  def writeStringToStderrAndExit(data: String, code: Int = 1): F[Unit]
  // TODO maybe here we should have either instead of unit
  def writeToFile(path: String, data: ByteString): F[Unit]
  def writeToFile(path: String, data: String): F[Unit]
  def exit(code: Int): F[Unit]
}
