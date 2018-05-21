package pravda.node.db.serialyzer

trait ValueWriter[A] extends ByteWriter[A]

object ValueWriter {
  implicit val nullWriter: ValueWriter[Null] = new ValueWriter[Null] {
    override def toBytes(x: Null): Array[Byte] = Array.empty[Byte]
  }
}
