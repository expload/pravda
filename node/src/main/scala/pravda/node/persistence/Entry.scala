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

import pravda.node.db.DB
import pravda.node.db.Operation.{Delete, Put}
import pravda.node.db.serialyzer.{KeyWriter, ValueReader, ValueWriter}
import shapeless.{::, HNil}

import scala.concurrent.Future

class Entry[K, V](
    db: DB,
    prefix: String,
    keyWriter: KeyWriter[String :: K :: HNil],
    valueWriter: ValueWriter[V],
    valueReader: ValueReader[V]
) {
  type KeyType = String :: K :: HNil

  def key(id: K): KeyType = prefix :: id :: HNil

  def put(id: K) = db.put(key(id))(keyWriter)

  def delete(id: K) = db.delete(key(id))(keyWriter)

  def contains(id: K): Future[Boolean] = db.contains(key(id))(keyWriter)

  def syncContains(id: K): Boolean = db.syncContains(key(id))(keyWriter)

  def get(id: K): Future[Option[V]] =
    db.getAs[V](key(id))(keyWriter, valueReader)

  def syncGet(id: K): Option[V] =
    db.syncGetAs[V](key(id))(keyWriter, valueReader)

  def put(id: K, value: V) =
    db.put(key(id), value)(keyWriter, valueWriter)

  def startsWith(id: K, offset: K, count: Long): Future[List[V]] = {
    println(key(id))
    println(key(offset))
    db.startsWithAs[V](key(id), key(offset), count)(keyWriter, valueReader)
  }

  def startsWith(id: K, offset: K): Future[List[V]] = {
    db.startsWithAs[V](key(id), key(offset))(keyWriter, valueReader)
  }

  def startsWith(id: K): Future[List[V]] = {
    db.startsWithAs[V](key(id))(keyWriter, valueReader)
  }

  def all(implicit stKeyWriter: KeyWriter[String]): Future[List[V]] = {
    db.startsWithAs[V](prefix)(stKeyWriter, valueReader)
  }

  def putBatch(id: K): Put = Put(key(id))(keyWriter)

  def putBatch(id: K, value: V): Put = Put(key(id), value)(keyWriter, valueWriter)

  def deleteBatch(id: K, value: V): Delete = Delete(key(id))(keyWriter)

}

object Entry {

  def apply[K, V](db: DB, prefix: String)(
      implicit
      keyWriter: KeyWriter[String :: K :: HNil],
      valueWriter: ValueWriter[V],
      valueReader: ValueReader[V]
  ) = new Entry(db, prefix, keyWriter, valueWriter, valueReader)
}

class SingleEntry[V](
    db: DB,
    key: String,
    keyWriter: KeyWriter[String],
    valueWriter: ValueWriter[V],
    valueReader: ValueReader[V]
) {

  def put(value: V): Future[Unit] = db.put(key, value)(keyWriter, valueWriter)

  def get(): Future[Option[V]] = db.getAs[V](key)(keyWriter, valueReader)

  def syncGet(): Option[V] = db.syncGetAs[V](key)(keyWriter, valueReader)

  def putBatch(value: V): Put = Put(key, value)(keyWriter, valueWriter)

  def deleteBatch(value: V): Delete = Delete(key)(keyWriter)

}

object SingleEntry {

  def apply[V](db: DB, key: String)(
      implicit keyWriter: KeyWriter[String],
      valueWriter: ValueWriter[V],
      valueReader: ValueReader[V]
  ) = new SingleEntry(db, key, keyWriter, valueWriter, valueReader)
}
