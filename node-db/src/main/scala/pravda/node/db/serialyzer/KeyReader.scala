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
