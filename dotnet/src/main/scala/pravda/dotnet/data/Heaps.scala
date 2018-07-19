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

package pravda.dotnet.data

import fastparse.byte.all._
import pravda.dotnet.utils._

object Heaps {
  private val blobBytes: P[Bytes] = P(Int8).flatMap(b => {
    if ((b & (1 << 7)) == 0) {
      val size = b.toInt
      AnyBytes(size).!
    } else if ((b & (1 << 6)) == 0) {
      P(Int8).flatMap(x => {
        val size = x + ((b & 0x3f) << 8)
        AnyBytes(size).!
      })
    } else {
      P(Int8 ~ Int8 ~ Int8).flatMap {
        case (x, y, z) =>
          val size = z + (y << 8) + (z << 16) + ((b & 0x1f) << 24)
          AnyBytes(size).!
      }
    }
  })

  def blob(blobHeap: Bytes, idx: Long): Either[String, Bytes] = {
    blobBytes.parse(blobHeap, idx.toInt).toEither
  }

  def string(stringHeap: Bytes, idx: Long): Either[String, String] =
    nullTerminatedString.parse(stringHeap, idx.toInt).toEither

  def userString(userStringHeap: Bytes, idx: Long): Either[String, String] =
    blobBytes
      .map(bs => new String(bs.dropRight(1L).toArray, "UTF-16LE"))
      .parse(userStringHeap, idx.toInt)
      .toEither

}
