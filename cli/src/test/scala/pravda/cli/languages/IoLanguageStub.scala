package pravda.cli.languages

import java.nio.charset.StandardCharsets

import cats.Id
import com.google.protobuf.ByteString

import scala.collection.mutable

final class IoLanguageStub(stdin: Option[ByteString] = None,
                           val files: mutable.Map[String, ByteString] = mutable.Map.empty)
    extends IoLanguage[Id] {

  import IoLanguageStub._

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

  def writeToFile(path: String, data: ByteString): Id[Unit] =
    files += (path -> data)

  def writeToFile(path: String, data: String): Id[Unit] =
    writeToFile(path, ByteString.copyFrom(data.getBytes(StandardCharsets.UTF_8)))

  def readFromStdin(): Id[ByteString] =
    stdin.getOrElse(ByteString.EMPTY)

  def createTmpDir(): Id[String] =
    "/tmp/"

  def readFromFile(path: String): Id[Option[ByteString]] =
    files.get(path)

  override def mkdirs(path: String): Id[Unit] = files.put(path, dir)

  override def pwd(): Id[String] = "/"

  override def isDirectory(path: String): Id[Option[Boolean]] =
    files.get(path).map(_ == dir)

  override def concatPath(parent: String, child: String): Id[String] =
    s"$parent/$child"

  def isFile(path: String): Id[Option[Boolean]] = files.get(path).map(_ != dir)

  def listFiles(dir: String): Id[List[String]] =
    if (isDirectory(dir).getOrElse(false)) {
      files.filterKeys(k => isFile(k).getOrElse(false)).keys.toList
    } else {
      List.empty
    }

  def listDirs(dir: String): Id[List[String]] = List.empty

  def absolutePath(path: String): Id[Option[String]] = Some(path)
}

object IoLanguageStub {
  final val dir: ByteString = ByteString.copyFromUtf8("dir")
}
