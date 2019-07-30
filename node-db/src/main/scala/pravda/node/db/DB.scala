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

import java.io.File

import org.iq80.leveldb._
import org.iq80.leveldb.impl.Iq80DBFactory._
import pravda.node.db.hash.utils._
import pravda.node.db.serialyzer._
import pravda.node.db.utils.ByteArray

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

object DB {

  def apply(path: String, initialHash: Option[Array[Byte]]): DB = {
    new DB(path, initialHash)
  }

  final case class Result(bytes: Array[Byte]) {
    def as[V](implicit valueReader: ValueReader[V]): V = valueReader.fromBytes(bytes)
  }

}

class DB(
    path: String,
    initialHash: Option[Array[Byte]]
) {

  import DB._

  private val options = new Options
  options.createIfMissing(true)
  private val db = factory.open(new File(path), options)

  def close(): Unit = {
    db.close()
  }

  // Resource management by @dkomanov
  // https://medium.com/@dkomanov/scala-try-with-resources-735baad0fd7d
  // TODO should be totally rewritten with akka streams.
  def tryClosable[T <: AutoCloseable, V](r: => T)(f: T => V): V = {
    def closeAndAddSuppressed(e: Throwable, resource: AutoCloseable): Unit = {
      if (e != null) {
        try {
          resource.close()
        } catch {
          case NonFatal(suppressed) =>
            e.addSuppressed(suppressed)
        }
      } else {
        resource.close()
      }
    }
    val resource: T = r
    require(resource != null, "resource is null")
    var exception: Throwable = null
    try {
      f(resource)
    } catch {
      case NonFatal(e) =>
        exception = e
        throw e
    } finally {
      closeAndAddSuppressed(exception, resource)
    }
  }

  // val lockedKeys = new java.util.concurrent.ConcurrentHashMap[Array[Byte], Unit]
  val lockedKeys: TrieMap[ByteArray, Unit] = scala.collection.concurrent.TrieMap.empty[ByteArray, Unit]
  private def lock[A](keys: Set[ByteArray])(block: => A): A = {
    val sortedKeys = keys.toVector.sorted
    try {
      // LOCK
      sortedKeys.foreach { key =>
        while (lockedKeys.putIfAbsent(key, ()).isDefined) {}
      }
      block
    } finally {
      // UNLOCK
      sortedKeys.foreach { key =>
        lockedKeys.remove(key)
      }
    }
  }

  def applyBatchOperations(operations: Operation*): Unit = {
    val keys = operations.map(_.key).map(ByteArray).toSet
    lock(keys) {
      val diff = operations
        .foldLeft((zeroHash, Map.empty[ByteArray, Operation])) {
          case ((currentDiff, stateCache), op) =>
            val diff = op.diff(() => Option(db.get(op.key)), stateCache)
            (xor(currentDiff, diff), stateCache + (ByteArray(op.key) -> op))
        }
        ._1

      applyDiff(diff)

      tryClosable(db.createWriteBatch()) { batch =>
        operations.foreach {
          case Operation.Delete(key) =>
            batch.delete(key)
          case Operation.Put(key, value) =>
            batch.put(key, value)
        }
        db.write(batch)
      }
    }
  }

  def applySingleOperation(operation: Operation) = {
    lock(Set(ByteArray(operation.key))) {
      val maybePrevValueFromDb = () => Option(db.get(operation.key))
      applyDiff(operation.diff(maybePrevValueFromDb))
      operation match {
        case Operation.Delete(key)     => db.delete(key)
        case Operation.Put(key, value) => db.put(key, value)
      }
    }
  }

  def deleteBytes(key: Array[Byte]): Unit = applySingleOperation(Operation.Delete(key))

  def putBytes(key: Array[Byte], value: Array[Byte]): Unit = applySingleOperation(Operation.Put(key, value))

  def delete[K](key: K)(implicit keyWriter: KeyWriter[K]): Unit = deleteBytes(keyWriter.toBytes(key))

  def put[K, V](key: K, value: V)(implicit keyWriter: KeyWriter[K], valueWriter: ValueWriter[V]): Unit =
    putBytes(keyWriter.toBytes(key), valueWriter.toBytes(value))

  def put[K](key: K)(implicit keyWriter: KeyWriter[K]): Unit = put(key, null)(keyWriter, ValueWriter.nullWriter)

  def get[K](key: K)(implicit keyWriter: KeyWriter[K]): Future[Option[Result]] = Future(syncGet(key))

  def syncGet[K](key: K)(implicit keyWriter: KeyWriter[K]): Option[Result] =
    Option(db.get(keyWriter.toBytes(key))).map(Result)

  def syncContains[K](key: K)(implicit keyWriter: KeyWriter[K]): Boolean = syncGet(key)(keyWriter).isDefined

  class SyncGetConstructor[V] {

    def apply[K](key: K)(implicit keyWriter: KeyWriter[K], valueReader: ValueReader[V]): Option[V] =
      syncGet[K](key)(keyWriter).map(_.as[V](valueReader))
  }

  def syncGetAs[V] = new SyncGetConstructor[V]

  class StartsConstructor[V] {

    def apply[K](prefix: K, offset: K, count: Long)(implicit keyWriter: KeyWriter[K],
                                                    valueReader: ValueReader[V]): Future[List[V]] =
      startsWith[K](prefix, offset, count)(keyWriter).map(_.map(_.as[V](valueReader)))

    def apply[K](prefix: K, offset: K)(implicit keyWriter: KeyWriter[K], valueReader: ValueReader[V]): Future[List[V]] =
      startsWith[K](prefix, offset)(keyWriter).map(_.map(_.as[V](valueReader)))

    def apply[K](prefix: K)(implicit keyWriter: KeyWriter[K], valueReader: ValueReader[V]): Future[List[V]] =
      startsWith[K](prefix)(keyWriter).map(_.map(_.as[V](valueReader)))
  }

  def startsWithAs[V] = new StartsConstructor[V]

  def startsWith(prefix: Array[Byte], offset: Array[Byte]): Stream[(Array[Byte], Array[Byte])] = {
    var it: DBIterator = null
    try {
      it = db.iterator()
      it.seek(prefix)
      def next(): Stream[(Array[Byte], Array[Byte])] = {
        try {
          if (!it.hasNext) Stream.empty
          else {
            val record = it.peekNext()
            val key = record.getKey
            if (key.startsWith(prefix)) {
              val value = record.getValue
              Stream((key, value)) ++ next
            } else {
              it.close()
              Stream.empty
            }
          }
        } catch {
          case e: Throwable =>
            if (it != null)
              it.close()
            throw e
        }
      }
      next()
    } catch {
      case e: Throwable =>
        if (it != null)
          it.close()
        throw e
    }
  }

  def startsWith[K](prefix: K, offset: K, count: Long)(implicit keyWriter: KeyWriter[K]): Future[List[Result]] =
    Future {
      val prefixBytes = keyWriter.toBytes(prefix)
      val offsetBytes = keyWriter.toBytes(offset)
      tryClosable(db.iterator()) { it =>
        it.seek(offsetBytes)
        val res = ListBuffer.empty[Result]
        while (res.length.toLong < count && it.hasNext && it.peekNext.getKey.startsWith(prefixBytes)) {
          val v = Result(it.peekNext.getValue)
          res += v
          it.next
        }
        res.toList
      }
    }

  def startsWith[K](prefix: K, offset: K)(implicit keyWriter: KeyWriter[K]): Future[List[Result]] =
    startsWith(prefix, offset, Long.MaxValue)

  def startsWith[K](prefix: K)(implicit keyWriter: KeyWriter[K]): Future[List[Result]] = startsWith(prefix, prefix)

  private var curHash = initialHash.getOrElse(zeroHash)

  def stateHash: Array[Byte] = synchronized { curHash }

  def applyDiff(diff: Array[Byte]): Unit = synchronized {
    curHash = xor(curHash, diff)
  }

}
