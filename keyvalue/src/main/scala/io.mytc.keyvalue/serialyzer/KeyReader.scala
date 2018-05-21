package io.mytc.keyvalue.serialyzer

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

trait KeyReader[A] extends ByteReader[A]

object KeyReader {

  implicit val stringReader: KeyReader[String] = new KeyReader[String] {
    override def fromBytes(array: Array[Byte]): String = ByteReader.read[String](array)
  }
  implicit val longReader: KeyReader[Long] = new KeyReader[Long] {
    override def fromBytes(array: Array[Byte]): Long = ByteReader.read[Long](array)
  }
  implicit val intReader: KeyReader[Int] = new KeyReader[Int] {
    override def fromBytes(array: Array[Byte]): Int = ByteReader.read[Int](array)
  }
  implicit val bytesReader: KeyReader[Array[Byte]] = new KeyReader[Array[Byte]] {
    override def fromBytes(array: Array[Byte]): Array[Byte] = array
  }
}
