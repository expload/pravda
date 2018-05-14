package io.mytc.keyvalue

import java.io.File

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import serialyzer._
import hash.utils._
import io.mytc.keyvalue.utils.ByteArray
import org.iq80.leveldb._
import org.iq80.leveldb.impl.Iq80DBFactory._

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future


object DB {

  def apply(path: String, hashCounter: Boolean = false): DB = {
    new DB(path, hashCounter)
  }
}

class DB (
      path: String,
      hashCounter: Boolean
  ) {

  private val options = new Options
  options.createIfMissing(true)
  private val db = factory.open(new File(path), options)

  def close(): Unit = {
    db.close()
  }

  private def tryCloseable[A <: AutoCloseable, B](resource: A)(block: A => B): B = {
    Try(block(resource)) match {
      case Success(result) =>
        resource.close()
        result
      case Failure(e) =>
        resource.close()
        throw e
    }
  }

  private def bArr(array: Array[Byte]): ByteArray = ByteArray(array)

  // val lockedKeys = new java.util.concurrent.ConcurrentHashMap[Array[Byte], Unit]
  val lockedKeys: TrieMap[ByteArray, Unit] = scala.collection.concurrent.TrieMap.empty[ByteArray, Unit]
  private def lock[A](keys: Set[ByteArray])(block: => A): A = {
    val sortedKeys = keys.toVector.sorted
    try {
      // LOCK
      sortedKeys.foreach {
        key =>
          while(lockedKeys.putIfAbsent(key, ()).isDefined) {}
      }
      block
    } finally {
      // UNLOCK
      sortedKeys.foreach {
        key =>
          lockedKeys.remove(key)
      }
    }
  }

  private def execWithLock[A](keys: Set[ByteArray])(diff: => Array[Byte], op: => A): A = {
    lock(keys) {
      if(hashCounter) {
          val d = diff
          val res = op
          applyDiff(d)
          res
      } else {
        op
      }
    }
  }
  private def execWithLock[A](key: ByteArray)(diff: => Array[Byte], op: => A): A= {
    execWithLock(Set(key))(diff, op)
  }
  private def exec[A](keys: Set[ByteArray])(diff: => Array[Byte], op: => A): A = {
    if(hashCounter) {
      lock(keys) {
        val d = diff
        val res = op
        applyDiff(d)
        res
      }
    } else {
      op
    }
  }
  private def exec[A](key: ByteArray)(diff: => Array[Byte], op: => A): A= {
    exec(Set(key))(diff, op)
  }

  def batchDiff(operatioins: Operation*): Array[Byte] = {
    operatioins.foldLeft((zeroHash, Map.empty[ByteArray, Operation])) {
      case ((currentDiff, stateCache), op) =>
      op match {
        case Operation.Delete(key) =>
          val diff = deleteDiff(key, stateCache)
          (xor(currentDiff, diff), stateCache + (bArr(key) -> op))
        case Operation.Put(key, value) =>
          val diff = putDiff(key, value, stateCache)
          (xor(currentDiff, diff), stateCache + (bArr(key) -> op))
      }
    }._1
  }
  def batch(operatioins: Operation*): Future[Unit] = Future {
    val keys = operatioins.map(_.key).map(bArr).toSet
    exec(keys)(
      batchDiff(operatioins:_*),
      {
        tryCloseable(db.createWriteBatch()) { batch =>
          operatioins.foreach {
            case Operation.Delete(key) =>
              batch.delete(key)
            case Operation.Put(key, value) =>
              batch.put(key, value)
          }
          db.write(batch)
        }
      }
    )
  }
  // def transaction(oprations: Operation*) = batch(oprations:_*)
  def deleteDiff(key: Array[Byte], cache: Map[ByteArray, Operation] = Map.empty): Array[Byte] = {
    def prevValue = cache
      .get(bArr(key))
      .map {
        case Operation.Put(_, v) => Some(v)
        case Operation.Delete(_) => None
      }
      .getOrElse(Option(db.get(key)))

    prevValue
      .map(hashPair(key, _))
      .getOrElse(zeroHash)
  }
  def deleteBytes(key: Array[Byte]): Future[Unit] = Future {
    exec(bArr(key))(
      deleteDiff(key),
      db.delete(key)
    )
  }
  def delete[K](key: K)(implicit keyWriter: KeyWriter[K]): Future[Unit] = {
    val keyBytes = keyWriter.toBytes(key)
    deleteBytes(keyBytes)
  }

  def putDiff(key: Array[Byte], value: Array[Byte], cache: Map[ByteArray, Operation] = Map.empty): Array[Byte] = {
    def prevValue = cache
      .get(bArr(key)).map {
        case Operation.Put(_, v) => Some(v)
        case Operation.Delete(_) => None
      }
      .getOrElse(Option(db.get(key)))
    val prevHash = prevValue
      .map(hashPair(key, _))
      .getOrElse(zeroHash)
    val newHash = hashPair(key, value)
    xor(prevHash, newHash)
  }
  def putBytes(key: Array[Byte], value: Array[Byte]): Future[Unit] = Future {
    exec(bArr(key))(
      putDiff(key, value),
      db.put(key, value)
    )
  }
  def put[K, V](key: K, value: V)(implicit keyWriter: KeyWriter[K], valueWriter: ValueWriter[V]): Future[Unit] = {
    val keyBytes = keyWriter.toBytes(key)
    val valueBytes = valueWriter.toBytes(value)
    putBytes(keyBytes, valueBytes)
  }
  def put[K](key: K)(implicit keyWriter: KeyWriter[K]): Future[Unit] = {
    // scalafix:off DisableSyntax.keywords.null
    put(key, null)(keyWriter, ValueWriter.nullWriter)
    // scalafix:on DisableSyntax.keywords.null
  }

  final case class Result(bytes: Array[Byte]) {
    def as[V](implicit valueReader: ValueReader[V]): V = valueReader.fromBytes(bytes)
  }

  def get[K](key: K)(implicit keyWriter: KeyWriter[K]): Future[Option[Result]] = Future {
    Option(db.get(keyWriter.toBytes(key))).map(Result)
  }
  def syncGet[K](key: K)(implicit keyWriter: KeyWriter[K]): Option[Result] = {
    Option(db.get(keyWriter.toBytes(key))).map(Result)
  }

  def contains[K](key: K)(implicit keyWriter: KeyWriter[K]): Future[Boolean] = {
    get(key)(keyWriter).map(_.isDefined)
  }
  def syncContains[K](key: K)(implicit keyWriter: KeyWriter[K]): Boolean = {
    syncGet(key)(keyWriter).isDefined
  }

  class GetConstructor[V] {
    def apply[K](key: K)(implicit keyWriter: KeyWriter[K], valueReader: ValueReader[V]): Future[Option[V]] =
      get[K](key)(keyWriter).map(_.map(_.as[V](valueReader)))
  }
  class SyncGetConstructor[V] {
    def apply[K](key: K)(implicit keyWriter: KeyWriter[K], valueReader: ValueReader[V]): Option[V] =
      syncGet[K](key)(keyWriter).map(_.as[V](valueReader))
  }


  def getAs[V] = new GetConstructor[V]
  def syncGetAs[V] = new SyncGetConstructor[V]

  class StartsConstructor[V] {
    def apply[K](key: K)(implicit keyWriter: KeyWriter[K], valueReader: ValueReader[V]): Future[List[V]] =
      startsWith[K](key)(keyWriter).map(_.map(_.as[V](valueReader)))
  }

  def startsWithAs[V] = new StartsConstructor[V]

  def startsWith[K](prefix: K)(implicit keyWriter: KeyWriter[K]) = Future {
    val keyBytes = keyWriter.toBytes(prefix)
    tryCloseable(db.iterator()) { it =>
      val it = db.iterator()
      it.seek(keyBytes)
      var res = Vector.empty[Result]
      while (it.hasNext && it.peekNext.getKey.startsWith(keyBytes)) {
        val v = Result(it.peekNext.getValue)
        res = res :+ v
        it.next
      }
      res.toList
    }
  }

  def incDiff(key: Array[Byte], cache: Map[ByteArray, Operation] = Map.empty): Array[Byte] = {
    val prevValue = cache
      .get(bArr(key))
      .map {
        case Operation.Put(_, v) => Some(v)
        case Operation.Delete(_) => None
      }
      .getOrElse(Option(db.get(key)))
    val prevValueOpt = prevValue
    val prevHash = prevValueOpt
      .map(hashPair(key, _))
      .getOrElse(zeroHash)
    val newValue = prevValueOpt.map(v => ByteReader.longReader.fromBytes(v) + 1).getOrElse(0L)
    val newValueBytes = ByteWriter.longWriter.toBytes(newValue)
    val newHash = hashPair(key, newValueBytes)
    xor(prevHash, newHash)
  }
  def inc[K](key: K)(implicit keyWriter: KeyWriter[K]): Future[Long] = Future {
    val keyBytes = keyWriter.toBytes(key)
    execWithLock(bArr(keyBytes)) (
      incDiff(keyBytes),
      {
        val prevValue = Option(db.get(keyBytes))
        val newValue = prevValue.map(v => ByteReader.longReader.fromBytes(v) + 1).getOrElse(0L)
        val newValueBytes = ByteWriter.longWriter.toBytes(newValue)
        db.put(keyBytes, newValueBytes)
        newValue
      }
    )
  }

  private var curHash = zeroHash

  private def calcHashSync: Array[Byte] = {

    tryCloseable(db.iterator()) { it =>
      it.seekToFirst()
      var hashSum = zeroHash

      while (it.hasNext) {
        val v = it.next
        val key = v.getKey
        val value = v.getValue
        val h = hashPair(key, value)
        hashSum = xor(hashSum, h)
      }
      hashSum
    }

  }
  def calcHash: Future[Array[Byte]] = Future {
    calcHashSync
  }
  def setHash(hash: Array[Byte]): Unit = synchronized {
    curHash = hash
  }
  def initHash(): Unit = synchronized {
    curHash = calcHashSync
  }
  def stateHash: Array[Byte] = synchronized { curHash }
  def applyDiff(diff: Array[Byte]): Unit = synchronized {
    curHash = xor(curHash, diff)
  }

}


