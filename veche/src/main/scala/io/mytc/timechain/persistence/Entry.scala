package io.mytc.timechain.persistence

import io.mytc.keyvalue.DB
import io.mytc.keyvalue.Operation.{Delete, Put}
import io.mytc.keyvalue.serialyzer.{KeyWriter, ValueReader, ValueWriter}
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

  def startsWith[P](st: P)(implicit stKeyWriter: KeyWriter[String :: P :: HNil]): Future[List[V]] = {
    db.startsWithAs[V](prefix :: st :: HNil)(stKeyWriter, valueReader)
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

  def put(value: V) = db.put(key, value)(keyWriter, valueWriter)

  def get() = db.getAs[V](key)(keyWriter, valueReader)

  def syncGet() = db.syncGetAs[V](key)(keyWriter, valueReader)

  def putBatch(value: V): Put = Put(key, value)(keyWriter, valueWriter)

  def deleteBatch(value: V): Delete = Delete(key)(keyWriter)

}

object SingleEntry {

  def apply[V](key: String)(
      implicit db: DB,
      keyWriter: KeyWriter[String],
      valueWriter: ValueWriter[V],
      valueReader: ValueReader[V]
  ) = new SingleEntry(db, key, keyWriter, valueWriter, valueReader)
}
