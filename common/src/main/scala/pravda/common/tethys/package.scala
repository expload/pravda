package pravda.common

import _root_.{tethys => ts}
import _root_.tethys._
import _root_.tethys.json4s._
import _root_.tethys.writers.tokens.SimpleTokenWriter
import _root_.tethys.readers.tokens.QueueIterator
import _root_.tethys.readers.FieldName
import org.json4s._

package object tethys extends TethysInstances {

  def json4sFormat[T: ts.JsonWriter: ts.JsonReader]: CustomSerializer[T] =
    new CustomSerializer[T](
      formats =>
        (
          {
            case j4s =>
              val writer = new SimpleTokenWriter
              j4s.writeJson(writer)
              val iter = QueueIterator(writer.tokens)
              ts.JsonReader[T].read(iter)(FieldName())
          },
          {
            case t: T =>
              val writer = new SimpleTokenWriter
              t.writeJson(writer)
              val iter = QueueIterator(writer.tokens)
              ts.JsonReader[JValue].read(iter)(FieldName())
          }
      ))

}
