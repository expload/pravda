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

package pravda.node.client

import com.google.protobuf.ByteString

import scala.language.higherKinds

trait IoLanguage[F[_]] {
  def mkdirs(path: String): F[Unit]
  def pwd(): F[String]
  def isDirectory(path: String): F[Option[Boolean]]
  def isFile(path: String): F[Option[Boolean]]
  def listFiles(dir: String): F[List[String]]
  def listDirs(dir: String): F[List[String]]
  def absolutePath(path: String): F[Option[String]]
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
