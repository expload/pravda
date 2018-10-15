package pravda.common.tethys

import com.google.protobuf.ByteString
import pravda.common.bytes.{byteString2hex, hex2byteString}
import supertagged.{Tagged, lifterF}
import tethys.{JsonReader, JsonWriter}
import tethys.commons.Token
import tethys.derivation.semiauto.{jsonReader, jsonWriter}
import tethys.readers.FieldName
import tethys.readers.tokens.TokenIterator
import tethys.writers.tokens.TokenWriter

trait TethysInstances {
  private def throwUtrj(token: Token) =
    throw new Exception(s"Unable to read JSON. Unexpected token $token")

  implicit def eitherReader[L: JsonReader, R: JsonReader]: JsonReader[Either[L, R]] = new JsonReader[Either[L, R]] {

    def read(it: TokenIterator)(implicit fieldName: FieldName): Either[L, R] = {
      if (it.currentToken().isObjectStart) {
        it.nextToken()
        if (it.currentToken().isFieldName) {
          it.fieldName() match {
            case "error" =>
              it.nextToken()
              val res = Left(JsonReader[L].read(it))
              it.nextToken()
              res
            case "result" =>
              it.nextToken()
              val res = Right(JsonReader[R].read(it))
              it.nextToken()
              res
          }
        } else throwUtrj(it.currentToken())
      } else throwUtrj(it.currentToken())
    }
  }

  implicit def eitherWriter[L: JsonWriter, R: JsonWriter]: JsonWriter[Either[L, R]] = new JsonWriter[Either[L, R]] {

    def write(value: Either[L, R], tw: TokenWriter): Unit = {
      tw.writeObjectStart()
      value match {
        case Left(l) =>
          tw.writeFieldName("error")
          JsonWriter[L].write(l, tw)
        case Right(r) =>
          tw.writeFieldName("result")
          JsonWriter[R].write(r, tw)
      }
      tw.writeObjectEnd()
    }
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

}
