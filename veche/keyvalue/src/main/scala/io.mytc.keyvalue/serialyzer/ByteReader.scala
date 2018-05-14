package io.mytc.keyvalue.serialyzer

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

trait ByteReader[A] {
  def fromBytes(array: Array[Byte]): A
}

object ByteReader {

  def read[A](array: Array[Byte])(implicit reader: ByteReader[A]): A = reader.fromBytes(array)

  implicit val stringReader = new ByteReader[String] {
    override def fromBytes(array: Array[Byte]): String = new String(array, StandardCharsets.UTF_8)
  }
  implicit val longReader = new ByteReader[Long] {
    override def fromBytes(array: Array[Byte]): Long = {
      val b = ByteBuffer.allocate(8)
      b.put(array).flip()
      b.getLong
    }
  }
  implicit val intReader = new ByteReader[Int] {
    override def fromBytes(array: Array[Byte]): Int = {
      val b = ByteBuffer.allocate(4)
      b.put(array).flip()
      b.getInt
    }
  }
  implicit val doubleReader = new ByteReader[Double] {
    override def fromBytes(array: Array[Byte]): Double = {
      val b = ByteBuffer.allocate(8)
      b.put(array).flip()
      b.getDouble
    }
  }
  implicit val booleanReader = new ByteReader[Boolean] {
    override def fromBytes(array: Array[Byte]): Boolean = {
      array(0) == 0.toByte
    }
  }
  implicit val bigDecimalReader = new ByteReader[BigDecimal] {
    override def fromBytes(array: Array[Byte]): BigDecimal = {
      BigDecimal(stringReader.fromBytes(array))
    }
  }
  implicit val byteReader = new ByteReader[Byte] {
    override def fromBytes(array: Array[Byte]): Byte= {
      array(0)
    }
  }
  implicit val bytesReader = new ByteReader[Array[Byte]] {
    override def fromBytes(array: Array[Byte]): Array[Byte] = {
      array
    }
  }
}
