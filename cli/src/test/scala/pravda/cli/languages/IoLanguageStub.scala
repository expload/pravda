package pravda.cli.languages

import cats.Id
import com.google.protobuf.ByteString

import scala.collection.mutable

final class IoLanguageStub(stdin: Option[ByteString] = None, val files: mutable.Map[String, ByteString] = mutable.Map.empty) extends IoLanguage[Id] {

  val stderr: mutable.Buffer[ByteString] =
    mutable.Buffer.empty

  val stdout: mutable.Buffer[ByteString] =
    mutable.Buffer.empty

  var exitCode: Int = 0

  def writeStringToStdout(data: String): Id[Unit] =
    stdout += ByteString.copyFromUtf8(data)

  def writeStringToStderrAndExit(data: String, code: Int): Id[Unit] = {
    exitCode = code
    stderr += ByteString.copyFromUtf8(data)
  }

  def exit(code: Int): Id[Unit] =
    exitCode = code

  def writeToStdout(data: ByteString): Id[Unit] =
    stdout += data

  def saveToFile(path: String, data: ByteString): Id[Unit] =
    files += (path -> data)

  def readFromStdin(): Id[ByteString] =
    stdin.getOrElse(ByteString.EMPTY)

  def createTmpDir(): Id[String] =
    "/tmp/"

  def readFromFile(path: String): Id[Option[ByteString]] =
    files.get(path)
}