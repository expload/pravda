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

  implicit def bjsonDecoder[T: JsonReader]: BJsonDecoder[T] = t =>
    new String(t, StandardCharsets.UTF_8).jsonAs[T].fold(throw _, identity)
}
