/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.cli.languages

package impl

import java.io.File
import java.nio.file.{Files, Paths}
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets

import com.google.protobuf.ByteString

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process.stdin

class IoLanguageImpl(implicit executionContext: ExecutionContext) extends IoLanguage[Future] {

  def mkdirs(path: String): Future[Unit] = Future {
    new File(path).mkdirs()
  }

  def pwd(): Future[String] = Future {
    new File("").getAbsolutePath
  }

  def isDirectory(path: String): Future[Option[Boolean]] = Future {
    val file = new File(path)
    if (file.exists()) Some(file.isDirectory)
    else None
  }

  def concatPath(parent: String, child: String): Future[String] = Future {
    new File(new File(parent), child).getAbsolutePath
  }

  def createTmpDir(): Future[String] = Future {
    Files.createTempDirectory("pravda-cli").toAbsolutePath.toString
  }

  def readFromStdin(): Future[ByteString] = Future {
    val buf = ByteBuffer.allocate(65536)
    val channel = Channels.newChannel(stdin)
    while (channel.read(buf) >= 0) ()
    buf.flip
    ByteString.copyFrom(buf)
  }

  def readFromFile(pathString: String): Future[Option[ByteString]] = Future {
    val path = Paths.get(pathString)
    if (Files.exists(path) && !Files.isDirectory(path)) {
      Some(ByteString.copyFrom(Files.readAllBytes(path)))
    } else {
      None
    }
  }

  def writeToStdout(data: ByteString): Future[Unit] =
    Future(sys.process.stdout.write(data.toByteArray))

  def writeStringToStdout(data: String): Future[Unit] =
    Future(sys.process.stdout.print(data))

  def writeStringToStderrAndExit(data: String, code: Int): Future[Unit] = Future {
    sys.process.stderr.print(data)
    sys.exit(code)
  }

  def writeToFile(pathString: String, data: ByteString): Future[Unit] = Future {
    val path = Paths.get(pathString)
    if (!Files.isDirectory(path)) {
      Files.write(path, data.toByteArray)
    }
  }

  def writeToFile(pathString: String, data: String): Future[Unit] = Future {
    writeToFile(pathString, ByteString.copyFrom(data.getBytes(StandardCharsets.UTF_8)))
  }

  def exit(code: Int): Future[Unit] = Future {
    sys.exit(code)
  }
}
