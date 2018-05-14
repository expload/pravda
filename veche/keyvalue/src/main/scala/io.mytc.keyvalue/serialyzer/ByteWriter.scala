package io.mytc.keyvalue.serialyzer

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

trait ByteWriter[A] {
  def toBytes(value: A): Array[Byte]
}

object ByteWriter {

  def write[A](value: A)(implicit writer: ByteWriter[A]): Array[Byte] = writer.toBytes(value)

  implicit val stringWriter: ByteWriter[String] = new ByteWriter[String] {
    override def toBytes(value: String): Array[Byte] = value.getBytes(StandardCharsets.UTF_8)
  }
  implicit val longWriter: ByteWriter[Long] = new ByteWriter[Long] {
    override def toBytes(value: Long): Array[Byte] = {
      val b = ByteBuffer.allocate(8)
      b.putLong(value).array()
    }
  }
  implicit val intWriter: ByteWriter[Int] = new ByteWriter[Int] {
    override def toBytes(value: Int): Array[Byte] = {
      val b = ByteBuffer.allocate(4)
      b.putInt(value).array()
    }
  }
  implicit val doubleWriter: ByteWriter[Double] = new ByteWriter[Double] {
    override def toBytes(value: Double): Array[Byte] = {
      val b = ByteBuffer.allocate(8)
      b.putDouble(value).array()
    }
  }
  implicit val booleanWriter: ByteWriter[Boolean] = new ByteWriter[Boolean] {
    override def toBytes(value: Boolean): Array[Byte] = {
      Array(
        if(value) 0.toByte else 1.toByte
      )
    }
  }
  implicit val bigDecimalWriter: ByteWriter[BigDecimal] = new ByteWriter[BigDecimal] {
    override def toBytes(value: BigDecimal): Array[Byte] = {
      stringWriter.toBytes(value.toString)
    }
  }
  implicit val byteWriter: ByteWriter[Byte] = new ByteWriter[Byte] {
    override def toBytes(byte: Byte): Array[Byte] = {
      Array(byte)
    }
  }
  implicit val bytesWriter: ByteWriter[Array[Byte]] = new ByteWriter[Array[Byte]] {
    override def toBytes(array: Array[Byte]): Array[Byte] = {
      array
    }
  }
}
