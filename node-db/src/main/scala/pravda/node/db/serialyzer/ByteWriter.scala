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
        if (value) 0.toByte else 1.toByte
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
