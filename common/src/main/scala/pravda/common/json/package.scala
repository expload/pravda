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
          },
          {
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
          },
          {
            case t: T =>
              val writer = new SimpleTokenWriter
              t.writeJson(writer)
              val iter = QueueIterator(writer.tokens)
              tethys.JsonReader[String].read(iter)(FieldName())
          }
        ))
}
