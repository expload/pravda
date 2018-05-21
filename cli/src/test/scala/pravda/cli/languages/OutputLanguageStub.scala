package pravda.cli.languages

import cats.Id
import com.google.protobuf.ByteString

import scala.collection.mutable

final class OutputLanguageStub extends OutputLanguage[Id] {

  val stdout: mutable.Buffer[ByteString] =
    mutable.Buffer.empty

  val files: mutable.Buffer[(String, ByteString)] =
    mutable.Buffer.empty

  def writeToStdout(data: ByteString): Id[Unit] =
    stdout += data

  def saveToFile(path: String, data: ByteString): Id[Unit] =
    files += (path -> data)
}