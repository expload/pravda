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

package pravda.node.data.serialization

import java.nio.charset.StandardCharsets

import tethys._
import tethys.jackson.jacksonTokenIteratorProducer
import tethys.jackson.pretty.prettyJacksonTokenWriterProducer

object bjson extends BJsonTranscoder

trait BJsonTranscoder {

  type BJsonEncoder[T] = Transcoder[T, BJson]
  type BJsonDecoder[T] = Transcoder[BJson, T]

  implicit def bjsonEncoder[T: JsonWriter]: BJsonEncoder[T] =
    t => BJson @@ t.asJson.getBytes(StandardCharsets.UTF_8)

  implicit def bjsonDecoder[T: JsonReader]: BJsonDecoder[T] =
    t => new String(t, StandardCharsets.UTF_8).jsonAs[T].fold(throw _, identity)
}
