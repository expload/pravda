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

package pravda.node

package persistence

import pravda.node.db.serialyzer.{KeyWriter, ValueReader, ValueWriter}
import pravda.node.data.serialization.{Bson, BsonTranscoder, CompositeTranscoder}
import data.serialization._

object implicits extends BsonTranscoder with CompositeTranscoder {

  implicit def keyWriter[T: CompositeEncoder]: KeyWriter[T] = new KeyWriter[T] {
    override def toBytes(value: T): Array[Byte] = transcode(value).to[Composite]
  }

  implicit def valueReader[T: BsonDecoder]: ValueReader[T] = new ValueReader[T] {
    override def fromBytes(array: Array[Byte]): T = transcode(Bson @@ array).to[T]
  }

  implicit def valueWriter[T: BsonEncoder]: ValueWriter[T] = new ValueWriter[T] {
    override def toBytes(value: T): Array[Byte] = transcode(value).to[Bson]
  }

}
