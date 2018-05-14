package io.mytc.keyvalue

import io.mytc.keyvalue.serialyzer.{KeyWriter, ValueWriter}

import scala.concurrent.Future

sealed trait Operation {
  val key: Array[Byte]
}

object Operation {

  class Delete(val key: Array[Byte]) extends Operation {
    def exec(implicit db: DB): Future[Unit] = db.deleteBytes(key)
  }

  class Put(val key: Array[Byte],val value: Array[Byte]) extends Operation {
    def exec(implicit db: DB): Future[Unit] = db.putBytes(key, value)
  }

  object Delete {
    def apply[K](key: K)(implicit keyWriter: KeyWriter[K]): Delete = {
      new Delete(keyWriter.toBytes(key))
    }
    def unapply(del: Delete): Option[Array[Byte]] = {
      Some(del.key)
    }
  }

  object Put {
    def apply[K, V](key: K, value: V)(implicit keyWriter: KeyWriter[K], valueWriter: ValueWriter[V]): Put = {
      new Put(keyWriter.toBytes(key), valueWriter.toBytes(value))
    }
    def apply[K](key: K)(implicit keyWriter: KeyWriter[K]): Put = {
      new Put(keyWriter.toBytes(key), Array.empty[Byte])
    }
    def unapply(put: Put): Option[Tuple2[Array[Byte], Array[Byte]]] = {
      Some((put.key, put.value))
    }
  }

}
