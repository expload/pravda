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

import zhukov.{Marshaller, Unmarshaller}
import pravda.common.{bytes => byteUtils}
import pravda.node.data.serialization.protobuf.{protobufEncoder, protobufDecoder}
import pravda.node.data.serialization.{Protobuf, transcode}
import pravda.node.db.serialyzer.KeyWriter
import pravda.node.db.{DB, Operation}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

trait DbPath {

  def mkKey(suffix: String): String

  def :+(suffix: String): DbPath

  def getAs[V: Unmarshaller](suffix: String): Option[V] =
    getRawBytes(suffix).map(arr => transcode[Protobuf](Protobuf @@ arr).to[V])

  def getRawBytes(suffix: String): Option[Array[Byte]]

  def put[V: Marshaller](suffix: String, value: V): Option[Array[Byte]] = {
    val bytes: Array[Byte] = transcode(value).to[Protobuf]
    putRawBytes(suffix, bytes)
  }

  def putRawBytes(suffix: String, value: Array[Byte]): Option[Array[Byte]]

  def remove(suffix: String): Option[Array[Byte]]

  def startsWith[V: Unmarshaller: Marshaller](suffix: String, offset: Long, count: Long)(
      implicit keyWriter: KeyWriter[String],
      ec: ExecutionContext): Future[List[V]]

  def startsWith[V: Unmarshaller: Marshaller](suffix: String, offset: Long)(implicit keyWriter: KeyWriter[String],
                                                                            ec: ExecutionContext): Future[List[V]] =
    startsWith(suffix, offset, Long.MaxValue)

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

  def startsWith[V: Unmarshaller: Marshaller](suffix: String, offset: Long, count: Long)(
      implicit keyWriter: KeyWriter[String],
      ec: ExecutionContext) = {
    // Delegate to pure db
    dbPath.startsWith(suffix, offset, count)
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

  def startsWith[V: Unmarshaller: Marshaller](suffix: String, offset: Long, count: Long)(
      implicit keyWriter: KeyWriter[String],
      ec: ExecutionContext): Future[List[V]] = {
    val key = mkKey(suffix)
    val offsetKey = key + ":" + f"$offset%016x"

    db.startsWith(key, offsetKey, count).map { records =>
      records.map(
        value =>
          transcode(Protobuf @@ value.bytes)
            .to[V])
    }
  }
}
