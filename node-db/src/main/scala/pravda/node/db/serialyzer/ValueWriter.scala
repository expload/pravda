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

trait ValueWriter[A] extends ByteWriter[A]

object ValueWriter {
  implicit val nullWriter: ValueWriter[Null] = new ValueWriter[Null] {
    override def toBytes(x: Null): Array[Byte] = Array.empty[Byte]
  }

  implicit val byteArrayWriter: ValueWriter[Array[Byte]] = new ValueWriter[Array[Byte]] {
    override def toBytes(array: Array[Byte]): Array[Byte] = array
  }

}
