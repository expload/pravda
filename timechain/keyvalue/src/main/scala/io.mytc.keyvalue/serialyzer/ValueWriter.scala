package io.mytc.keyvalue.serialyzer

trait ValueWriter[A] extends ByteWriter[A]

object ValueWriter {
  implicit val nullWriter = new ValueWriter[Null] {
    override def toBytes(x: Null): Array[Byte] = Array.empty[Byte]
  }
}
