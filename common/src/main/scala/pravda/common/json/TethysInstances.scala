package pravda.common.json

import com.google.protobuf.ByteString
import pravda.common.bytes.{byteString2hex, hex2byteString}
import supertagged.{Tagged, lifterF}
import tethys.{JsonReader, JsonWriter}
import tethys.commons.Token
import tethys.derivation.semiauto.{jsonReader, jsonWriter}
import tethys.readers.FieldName
import tethys.readers.tokens.TokenIterator
import tethys.writers.tokens.TokenWriter

import scala.collection.mutable

trait TethysInstances {
  private def throwUtrj(token: Token) =
    throw new Exception(s"Unable to read JSON. Unexpected token $token")

  def sealedTraitReader[T](f: FieldName => PartialFunction[(TokenIterator, String), T]): JsonReader[T] =
    new JsonReader[T] {

      def read(it: TokenIterator)(implicit fieldName: FieldName): T = {
        if (it.currentToken().isObjectStart) {
          it.nextToken()
          if (it.currentToken().isFieldName) {
            val n = it.fieldName()
            it.nextToken()
            val tpl = (it, n)
            val pf = f(fieldName)
            val res =
              if (pf.isDefinedAt(tpl)) pf(tpl)
              else throw new Exception(s"Unexpected field: $n")
            it.nextToken()
            res
          } else throwUtrj(it.currentToken())
        } else throwUtrj(it.currentToken())
      }
    }

  implicit def eitherReader[L: JsonReader, R: JsonReader]: JsonReader[Either[L, R]] =
    sealedTraitReader { implicit fieldName =>
      {
        case (it, "failure") => Left(JsonReader[L].read(it))
        case (it, "success") => Right(JsonReader[R].read(it))
      }
    }

  implicit def eitherWriter[L: JsonWriter, R: JsonWriter]: JsonWriter[Either[L, R]] =
    (value: Either[L, R], tw: TokenWriter) => {
      tw.writeObjectStart()
      value match {
        case Left(l)  => JsonWriter[L].write("failure", l, tw)
        case Right(r) => JsonWriter[R].write("success", r, tw)
      }
      tw.writeObjectEnd()
    }

  implicit def tuple2Reader[T1: JsonReader, T2: JsonReader]: JsonReader[(T1, T2)] =
    jsonReader[Tuple2[T1, T2]]

  implicit def tuple2Writer[T1: JsonWriter, T2: JsonWriter]: JsonWriter[(T1, T2)] =
    jsonWriter[Tuple2[T1, T2]]

  //----------------------------------------------------------------------
  // Supertagged support for tethys
  //----------------------------------------------------------------------

  implicit def tethysWriterLifter[T: JsonWriter, U]: JsonWriter[Tagged[T, U]] =
    lifterF[JsonWriter].lift[T, U]

  implicit def tethysReaderLifter[T: JsonReader, U]: JsonReader[Tagged[T, U]] =
    lifterF[JsonReader].lift[T, U]

  //----------------------------------------------------------------------
  // Protobufs' ByteString support for tethys
  //----------------------------------------------------------------------

  implicit val protobufByteStringReader: JsonReader[ByteString] =
    JsonReader.stringReader.map(hex2byteString)

  implicit val protobufByteStringWriter: JsonWriter[ByteString] =
    JsonWriter.stringWriter.contramap(byteString2hex)

  //---------------------------------------------------------------------------
  // scala.Map support
  //---------------------------------------------------------------------------

  trait MapKeySupport[T] {
    def show(x: T): String
    def mk(x: String): T
  }

  object MapKeySupport {

    def apply[T: MapKeySupport]: MapKeySupport[T] =
      implicitly[MapKeySupport[T]]
  }

  implicit def mapReader[K: MapKeySupport, V: JsonReader]: JsonReader[Map[K, V]] = new JsonReader[Map[K, V]] {

    def read(it: TokenIterator)(implicit fieldName: FieldName): Map[K, V] = {
      val token = it.currentToken()
      val res = mutable.Map.empty[K, V]
      if (token.isObjectStart) {
        var ct = it.nextToken()
        while (!ct.isObjectEnd) {
          val k = MapKeySupport[K].mk(it.fieldName())
          it.nextToken()
          res.put(k, JsonReader[V].read(it))
          ct = it.currentToken()
        }
        it.nextToken()
      } else throwUtrj(token)
      res.toMap
    }
  }

  implicit def mapWriter[K: MapKeySupport, V: JsonWriter]: JsonWriter[Map[K, V]] = new JsonWriter[Map[K, V]] {

    def write(value: Map[K, V], w: TokenWriter): Unit = {
      val ms = MapKeySupport[K]
      w.writeObjectStart()
      value.foreach {
        case (k, v) =>
          JsonWriter[V].write(ms.show(k), v, w)
      }
      w.writeObjectEnd()
    }
  }

  implicit val intMapSupport = new MapKeySupport[Int] {
    def show(x: Int): String = x.toString
    def mk(x: String): Int = x.toInt
  }
}
