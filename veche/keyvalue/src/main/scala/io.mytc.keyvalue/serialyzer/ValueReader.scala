package io.mytc.keyvalue.serialyzer

trait ValueReader[A] extends ByteReader[A]

object ValueReader {
  implicit val nullReader: ValueReader[Null] = new ValueReader[Null] {
    override def fromBytes(array: Array[Byte]): Null = null
  }
}
