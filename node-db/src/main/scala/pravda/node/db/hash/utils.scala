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
import java.security.MessageDigest

object utils {
  val DIGEST = MessageDigest.getInstance("SHA-256")

  def hex(arr: Array[Byte]) = String.format("%032x", new BigInteger(1, arr))

  def hash(value: Array[Byte]): Array[Byte] = DIGEST.digest(value)

  def hash(value: String, charset: String = StandardCharsets.UTF_8.name()): Array[Byte] =
    hash(value.getBytes(charset))

  def hashPair(key: Array[Byte], value: Array[Byte]): Array[Byte] = {
    hash(hash(key) ++ hash(value))
  }

  def hashPair(key: String, value: String): Array[Byte] = {
    hash(hash(key) ++ hash(value))
  }

  def xor(arr1: Array[Byte], arr2: Array[Byte]): Array[Byte] = {
    arr1.zip(arr2).map {
      case (el1, el2) =>
        (el1 ^ el2).toByte
    }
  }

  lazy val zeroHash = Array.fill[Byte](DIGEST.getDigestLength)(0.toByte)

}
