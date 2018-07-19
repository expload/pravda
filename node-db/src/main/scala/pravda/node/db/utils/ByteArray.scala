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

package pravda.node.db.utils

final case class ByteArray(value: Array[Byte]) extends Ordered[ByteArray] {
  override def hashCode(): Int = java.util.Arrays.hashCode(value)
  override def equals(other: Any): Boolean = other match {
    case o: Array[Byte] => java.util.Arrays.equals(value, o)
    case o: ByteArray   => java.util.Arrays.equals(value, o.value)
    case _              => false
  }

  override def compare(o: ByteArray): Int = {
    value
      .zip(o.value)
      .toStream
      .find {
        case (b1, b2) => b1 != b2
      }
      .map {
        case (b1, b2) => b1.compareTo(b2)
      }
      .getOrElse(
        value.length.compareTo(o.value.length)
      )
  }

  def inc(): ByteArray = {
    val idx = value.lastIndexWhere(b => (b + 1).toByte != 0)
    val tail = Array.fill(value.length - (idx + 1))(0.toByte)
    if (idx < 0) {
      ByteArray(tail)
    } else {
      val head = value.take(idx)
      val upd = (value(idx) + 1).toByte
      ByteArray((head :+ upd) ++ tail)
    }
  }

}
