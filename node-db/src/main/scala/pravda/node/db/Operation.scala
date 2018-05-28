package pravda.node.db

import pravda.node.db.serialyzer.{KeyWriter, ValueWriter}

import scala.concurrent.Future

sealed trait Operation {
  val key: Array[Byte]
}

object Operation {

  final case class Delete(key: Array[Byte]) extends Operation

  final case class Put(key: Array[Byte], value: Array[Byte]) extends Operation

  object Delete {

    def apply[K](key: K)(implicit keyWriter: KeyWriter[K]): Delete = {
      new Delete(keyWriter.toBytes(key))
    }
  }

  object Put {

    def apply[K, V](key: K, value: V)(implicit keyWriter: KeyWriter[K], valueWriter: ValueWriter[V]): Put = {
      new Put(keyWriter.toBytes(key), valueWriter.toBytes(value))
    }

    def apply[K](key: K)(implicit keyWriter: KeyWriter[K]): Put = {
      new Put(keyWriter.toBytes(key), Array.empty[Byte])
    }
  }

}
