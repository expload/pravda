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

package pravda.node.persistence

import pravda.node.data.serialization.bson.{BsonDecoder, BsonEncoder}
import pravda.node.data.serialization.{Bson, transcode}
import pravda.node.db.{DB, Operation}

import scala.collection.mutable
import pravda.common.{bytes => byteUtils}

trait DbPath {

  def mkKey(suffix: String): String

  def :+(suffix: String): DbPath

  def getAs[V: BsonDecoder](suffix: String): Option[V] =
    getRawBytes(suffix).map(arr => transcode(Bson @@ arr).to[V])

  def getRawBytes(suffix: String): Option[Array[Byte]]

  def put[V: BsonEncoder](suffix: String, value: V): Option[Array[Byte]] = {
    val bsonValue: Array[Byte] = transcode(value).to[Bson]
    putRawBytes(suffix, bsonValue)
  }

  def putRawBytes(suffix: String, value: Array[Byte]): Option[Array[Byte]]

  def remove(suffix: String): Option[Array[Byte]]

  protected def returningPrevious(suffix: String)(f: => Unit): Option[Array[Byte]] = {
    val prev = getRawBytes(suffix)
    f
    prev
  }
}

class CachedDbPath(dbPath: DbPath,
                   dbCache: mutable.Map[String, Option[Array[Byte]]],
                   dbOperations: mutable.Buffer[Operation])
    extends DbPath {
  def mkKey(suffix: String): String = dbPath.mkKey(suffix)

  def :+(suffix: String) = new CachedDbPath(dbPath :+ suffix, dbCache, dbOperations)

  def getRawBytes(suffix: String): Option[Array[Byte]] = {
    val key = mkKey(suffix)
    dbCache.get(key).orElse(Option(dbPath.getRawBytes(suffix))).flatten
  }

  def putRawBytes(suffix: String, value: Array[Byte]): Option[Array[Byte]] = returningPrevious(suffix) {
    val key = mkKey(suffix)
    dbCache.put(key, Some(value))
    dbOperations += Operation.Put(byteUtils.stringToBytes(key), value)
  }

  def remove(suffix: String): Option[Array[Byte]] = returningPrevious(suffix) {
    val key = mkKey(suffix)
    dbCache.put(key, None)
  }

}

class PureDbPath(db: DB, path: String) extends DbPath {

  def mkKey(suffix: String) = s"$path:$suffix"

  def :+(suffix: String) = new PureDbPath(db, mkKey(suffix))

  def getRawBytes(suffix: String): Option[Array[Byte]] = {
    val key = mkKey(suffix)
    db.syncGet(byteUtils.stringToBytes(key)).map(_.bytes)
  }

  def putRawBytes(suffix: String, value: Array[Byte]): Option[Array[Byte]] = returningPrevious(suffix) {
    val key = mkKey(suffix)
    db.syncPutBytes(byteUtils.stringToBytes(key), value)
  }

  def remove(suffix: String): Option[Array[Byte]] = returningPrevious(suffix) {
    val key = mkKey(suffix)
    db.syncDeleteBytes(byteUtils.stringToBytes(key))
  }

}
