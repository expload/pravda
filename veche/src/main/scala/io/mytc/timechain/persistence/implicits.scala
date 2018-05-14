package io.mytc
package timechain
package persistence

import io.mytc.keyvalue.serialyzer.{KeyWriter, ValueReader, ValueWriter}
import io.mytc.timechain.data.serialization.{Bson, BsonTranscoder, CompositeTranscoder}
import data.serialization._

object implicits extends BsonTranscoder with CompositeTranscoder {

  implicit def keyWriter[T: CompositeEncoder] = new KeyWriter[T] {
    override def toBytes(value: T): Array[Byte] = transcode(value).to[Composite]
  }

  implicit def valueReader[T: BsonDecoder] = new ValueReader[T] {
    override def fromBytes(array: Array[Byte]): T = transcode(Bson @@ array).to[T]
  }

  implicit def valueWriter[T: BsonEncoder] = new ValueWriter[T] {
    override def toBytes(value: T): Array[Byte] = transcode(value).to[Bson]
  }

}
