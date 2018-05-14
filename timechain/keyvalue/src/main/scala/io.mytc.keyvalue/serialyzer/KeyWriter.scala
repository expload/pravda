package io.mytc.keyvalue.serialyzer

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

trait KeyWriter[A] extends ByteWriter[A]

object KeyWriter {

  implicit val stringWriter = new KeyWriter[String] {
    override def toBytes(value: String): Array[Byte] = ByteWriter.stringWriter.toBytes(value)
  }
  implicit val longWriter = new KeyWriter[Long] {
    override def toBytes(value: Long): Array[Byte] = ByteWriter.longWriter.toBytes(value)
  }
  implicit val intWriter = new KeyWriter[Int] {
    override def toBytes(value: Int): Array[Byte] = ByteWriter.intWriter.toBytes(value)
  }
  implicit val bigDecimalWriter = new KeyWriter[BigDecimal] {
    override def toBytes(value: BigDecimal): Array[Byte] = ByteWriter.bigDecimalWriter.toBytes(value)
  }
  implicit val doubleWriter = new KeyWriter[Double] {
    override def toBytes(value: Double): Array[Byte] = ByteWriter.doubleWriter.toBytes(value)
  }
  implicit val booleanWriter = new KeyWriter[Boolean] {
    override def toBytes(value: Boolean): Array[Byte] = ByteWriter.booleanWriter.toBytes(value)
  }
  implicit val byteWriter = new KeyWriter[Byte] {
    override def toBytes(value: Byte): Array[Byte] = ByteWriter.byteWriter.toBytes(value)
  }
  implicit val bytesWriter = new KeyWriter[Array[Byte]] {
    override def toBytes(array: Array[Byte]): Array[Byte] = array
  }

}
