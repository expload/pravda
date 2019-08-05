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

package pravda.node.db.hash

import java.math.BigInteger
import java.nio.charset.StandardCharsets

import pravda.common.Hasher

object utils {

  def hex(arr: Array[Byte]) = String.format("%032x", new BigInteger(1, arr))

  def hash(value: Array[Byte]): Array[Byte] = Hasher.sha256.get().digest(value)

  def hash(value: String, charset: String = StandardCharsets.UTF_8.name()): Array[Byte] =
    hash(value.getBytes(charset))

  def hashPair(key: Array[Byte], value: Array[Byte]): Array[Byte] = {
    hash(hash(key) ++ hash(value))
  }

  def xor(arr1: Array[Byte], arr2: Array[Byte]): Array[Byte] = {
    arr1.zip(arr2).map {
      case (el1, el2) =>
        (el1 ^ el2).toByte
    }
  }

  lazy val zeroHash = Array.fill[Byte](Hasher.sha256.get().getDigestLength)(0.toByte)

}
