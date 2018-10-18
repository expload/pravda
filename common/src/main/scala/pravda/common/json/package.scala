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

package pravda.common

import tethys._
import tethys.json4s._
import tethys.writers.tokens.SimpleTokenWriter
import tethys.readers.tokens.QueueIterator
import tethys.readers.FieldName
import org.json4s._

package object json extends TethysInstances {

  def json4sFormat[T: tethys.JsonWriter: tethys.JsonReader: Manifest]: CustomSerializer[T] =
    new CustomSerializer[T](
      formats =>
        (
          {
            case j4s =>
              val writer = new SimpleTokenWriter
              j4s.writeJson(writer)
              val iter = QueueIterator(writer.tokens)
              tethys.JsonReader[T].read(iter)(FieldName())
          }, {
            case t: T =>
              val writer = new SimpleTokenWriter
              t.writeJson(writer)
              val iter = QueueIterator(writer.tokens)
              tethys.JsonReader[JValue].read(iter)(FieldName())
          }
      ))

  def json4sKeyFormat[T: tethys.JsonWriter: tethys.JsonReader: Manifest]: CustomKeySerializer[T] =
    new CustomKeySerializer[T](
      formats =>
        (
          {
            case j4s =>
              val writer = new SimpleTokenWriter
              j4s.writeJson(writer)
              val iter = QueueIterator(writer.tokens)
              tethys.JsonReader[T].read(iter)(FieldName())
          }, {
            case t: T =>
              val writer = new SimpleTokenWriter
              t.writeJson(writer)
              val iter = QueueIterator(writer.tokens)
              tethys.JsonReader[String].read(iter)(FieldName())
          }
      ))
}
