package pravda.node.db.serialyzer

trait KeyWriter[A] extends ByteWriter[A]

object KeyWriter {

  implicit val bytesWriter: KeyWriter[Array[Byte]] = new KeyWriter[Array[Byte]] {
    override def toBytes(array: Array[Byte]): Array[Byte] = array
  }

}
