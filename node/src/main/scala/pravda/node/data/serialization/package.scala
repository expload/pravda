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

package pravda.node.data

import supertagged.TaggedType

/**
  * Unified interface for binary and json encoding/decoding
  */
package object serialization {

  object Json extends TaggedType[String]
  type Json = Json.Type

  object BooPickle extends TaggedType[Array[Byte]]
  type BooPickle = BooPickle.Type

  object Bson extends TaggedType[Array[Byte]]
  type Bson = Bson.Type

  object Composite extends TaggedType[Array[Byte]]
  type Composite = Composite.Type

  def transcode[From](value: From): TranscodingDsl[From] = new TranscodingDsl(value)
}
