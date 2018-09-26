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

import pravda.node.db.serialyzer.{KeyWriter, ValueWriter}

sealed trait Operation {
  val key: Array[Byte]
}

object Operation {

  final case class Delete(key: Array[Byte]) extends Operation

  final case class Put(key: Array[Byte], value: Array[Byte]) extends Operation

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
