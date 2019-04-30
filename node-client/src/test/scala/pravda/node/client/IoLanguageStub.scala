package pravda.node.client

import java.nio.charset.StandardCharsets

import cats._
import com.google.protobuf.ByteString

import scala.collection.mutable

import scala.language.higherKinds

final class IoLanguageStub[F[_]: Monad](stdin: Option[ByteString] = None,
                                        val files: mutable.Map[String, ByteString] = mutable.Map.empty)
    extends IoLanguage[F] {

  import IoLanguageStub._

  val stderr: mutable.Buffer[ByteString] =
    mutable.Buffer.empty

  val stdout: mutable.Buffer[ByteString] =
    mutable.Buffer.empty

  var exitCode: Int = 0

  def writeStringToStdout(data: String): F[Unit] = Monad[F].pure {
    stdout += ByteString.copyFromUtf8(data)
  }

  def writeStringToStderrAndExit(data: String, code: Int): F[Unit] = Monad[F].pure {
    exitCode = code
    stderr += ByteString.copyFromUtf8(data)
  }

  def exit(code: Int): F[Unit] = Monad[F].pure {
    exitCode = code
  }

  def writeToStdout(data: ByteString): F[Unit] = Monad[F].pure {
    stdout += data
  }

  def writeToFile(path: String, data: ByteString): F[Unit] = Monad[F].pure {
    files += (path -> data)
  }

  def writeToFile(path: String, data: String): F[Unit] = Monad[F].pure {
    writeToFile(path, ByteString.copyFrom(data.getBytes(StandardCharsets.UTF_8)))
  }

  def readFromStdin(): F[ByteString] = Monad[F].pure {
    stdin.getOrElse(ByteString.EMPTY)
  }

  def createTmpDir(): F[String] = Monad[F].pure {
    "/tmp/"
  }

  def readFromFile(path: String): F[Option[ByteString]] = Monad[F].pure {
    files.get(path)
  }

  override def mkdirs(path: String): F[Unit] = Monad[F].pure {
    files.put(path, dir)
  }

  override def pwd(): F[String] = Monad[F].pure {
    "/"
  }

  override def isDirectory(path: String): F[Option[Boolean]] = Monad[F].pure {
    files.get(path).map(_ == dir)
  }

  override def concatPath(parent: String, child: String): F[String] = Monad[F].pure {
    s"$parent/$child"
  }

  def isFile(path: String): F[Option[Boolean]] = Monad[F].pure {
    files.get(path).map(_ != dir)
  }

  def listFiles(dir: String): F[List[String]] = Monad[F].pure {
    if (files.get(dir).contains(IoLanguageStub.dir)) {
      files.toList
        .filter {
          case (k, v) => v == IoLanguageStub.dir
        }
        .map(_._1)
    } else {
      List.empty
    }
  }

  def listDirs(dir: String): F[List[String]] = Monad[F].pure {
    List.empty
  }

  def absolutePath(path: String): F[Option[String]] = Monad[F].pure {
    Some(path)
  }
}

object IoLanguageStub {
  final val dir: ByteString = ByteString.copyFromUtf8("dir")
}
