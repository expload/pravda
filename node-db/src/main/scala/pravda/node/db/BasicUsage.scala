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

package pravda.node.db

import pravda.node.db.serialyzer._

import scala.concurrent.duration.Duration
import scala.concurrent.Await

object BasicUsage {

  import hash.utils._
  import scala.concurrent.ExecutionContext.Implicits.global

  def printHash(str: String = ""): Unit = {
    println(s"$str: ${hex(db.stateHash)}")
  }

  implicit val strKeyWriter: KeyWriter[String] = (value: String) => ByteWriter.stringWriter.toBytes(value)
  implicit val strValueWriter: ValueWriter[String] = (value: String) => ByteWriter.stringWriter.toBytes(value)
  implicit val strValueReader: ValueReader[String] = (value: Array[Byte]) => ByteReader.stringReader.fromBytes(value)

  val db = DB("/tmp/testdb", None)

  val f = for {
    _ <- db.put("newkey", "newvalue2")
    v <- db.getAs[String]("newkey")
  } yield (v)
  println(Await.result(f, Duration.Inf))

}
