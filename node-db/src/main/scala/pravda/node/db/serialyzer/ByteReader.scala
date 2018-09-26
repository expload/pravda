/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.node.db.serialyzer

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

trait ByteReader[A] {
  def fromBytes(array: Array[Byte]): A
}

object ByteReader {

  def read[A](array: Array[Byte])(implicit reader: ByteReader[A]): A = reader.fromBytes(array)

  implicit val stringReader: ByteReader[String] = new ByteReader[String] {
    override def fromBytes(array: Array[Byte]): String = new String(array, StandardCharsets.UTF_8)
  }
  implicit val longReader: ByteReader[Long] = new ByteReader[Long] {
    override def fromBytes(array: Array[Byte]): Long = {
      val b = ByteBuffer.allocate(8)
      b.put(array).flip()
      b.getLong
    }
  }
  implicit val intReader: ByteReader[Int] = new ByteReader[Int] {
    override def fromBytes(array: Array[Byte]): Int = {
      val b = ByteBuffer.allocate(4)
      b.put(array).flip()
      b.getInt
    }
  }
  implicit val doubleReader: ByteReader[Double] = new ByteReader[Double] {
    override def fromBytes(array: Array[Byte]): Double = {
      val b = ByteBuffer.allocate(8)
      b.put(array).flip()
      b.getDouble
    }
  }
  implicit val booleanReader: ByteReader[Boolean] = new ByteReader[Boolean] {
    override def fromBytes(array: Array[Byte]): Boolean = {
      array(0) == 0.toByte
    }
  }
  implicit val bigDecimalReader: ByteReader[BigDecimal] = new ByteReader[BigDecimal] {
    override def fromBytes(array: Array[Byte]): BigDecimal = {
      BigDecimal(stringReader.fromBytes(array))
    }
  }
  implicit val byteReader: ByteReader[Byte] = new ByteReader[Byte] {
    override def fromBytes(array: Array[Byte]): Byte = {
      array(0)
    }
  }
  implicit val bytesReader: ByteReader[Array[Byte]] = new ByteReader[Array[Byte]] {
    override def fromBytes(array: Array[Byte]): Array[Byte] = {
      array
    }
  }
}
