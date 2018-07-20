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

package pravda.node

package persistence

import java.io.{File, PrintWriter}

import pravda.node.data.common.ApplicationStateInfo
import pravda.node.data.serialization._
import pravda.node.data.serialization.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

object FileStore {

  import Config._

  private lazy val applicationStateInfoFile = new File(pravdaConfig.dataDirectory, "application-state-info.json")

  def readApplicationStateInfoAsync() = Future(readApplicationStateInfo())

  def readApplicationStateInfo(): Option[ApplicationStateInfo] = {
    if (applicationStateInfoFile.exists()) {
      Some(transcode(Json @@ Source.fromFile(applicationStateInfoFile).mkString).to[ApplicationStateInfo])
    } else {
      None
    }
  }

  def updateApplicationStateInfoAsync(settings: ApplicationStateInfo) = Future(updateApplicationStateInfo(settings))

  def updateApplicationStateInfo(settings: ApplicationStateInfo): Unit = {
    val pw = new PrintWriter(applicationStateInfoFile)
    try {
      pw.write(transcode(settings).to[Json])
    } finally {
      pw.close()
    }
  }

}
