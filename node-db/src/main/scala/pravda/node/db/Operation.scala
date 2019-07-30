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

package pravda.node.db

import pravda.node.db.hash.utils._
import pravda.node.db.serialyzer.{KeyWriter, ValueWriter}
import pravda.node.db.utils.ByteArray

sealed trait Operation {
  val key: Array[Byte]

  def diff(getMaybePrevValueFromDb: () => Option[Array[Byte]],
           cache: Map[ByteArray, Operation] = Map.empty): Array[Byte]
}

object Operation {
  private def maybePrevValueFromCache(key: Array[Byte], cache: Map[ByteArray, Operation]): Option[Array[Byte]] = {
    cache
      .get(ByteArray(key))
      .flatMap {
        case Operation.Put(_, v) => Some(v)
        case Operation.Delete(_) => None
      }
  }

  final case class Delete(key: Array[Byte]) extends Operation {

    def diff(maybePrevValueFromDb: () => Option[Array[Byte]],
             cache: Map[ByteArray, Operation] = Map.empty): Array[Byte] = {
      maybePrevValueFromCache(key, cache).orElse(maybePrevValueFromDb()).map(hashPair(key, _)).getOrElse(zeroHash)
    }
  }

  final case class Put(key: Array[Byte], value: Array[Byte]) extends Operation {

    def diff(getMaybePrevValueFromDb: () => Option[Array[Byte]],
             cache: Map[ByteArray, Operation] = Map.empty): Array[Byte] = {
      val maybePrevValue = maybePrevValueFromCache(key, cache).orElse(getMaybePrevValueFromDb())
      val newHash = hashPair(key, value)

      maybePrevValue.fold(newHash) { pValue =>
        // If newValue and previousValue are equal then hashPair will be equal too, so there is no difference
        if (value sameElements pValue) {
          zeroHash
        } else {
          val prevHash = hashPair(key, pValue)
          xor(prevHash, newHash)
        }
      }
    }
  }

  object Delete {

    def apply[K](key: K)(implicit keyWriter: KeyWriter[K]): Delete = {
      new Delete(keyWriter.toBytes(key))
    }
  }

  object Put {

    def apply[K, V](key: K, value: V)(implicit keyWriter: KeyWriter[K], valueWriter: ValueWriter[V]): Put = {
      new Put(keyWriter.toBytes(key), valueWriter.toBytes(value))
    }

    def apply[K](key: K)(implicit keyWriter: KeyWriter[K]): Put = {
      new Put(keyWriter.toBytes(key), Array.empty[Byte])
    }
  }

}
