package pravda.node.client

import cats.Id
import com.google.protobuf.ByteString

import scala.util.Random

final class PredictableRandomLanguage(seed: Int) extends RandomLanguage[Id] {

  private val random = new Random(seed)

  def secureBytes64(): Id[ByteString] = {
    val bytes = new Array[Byte](64)
    random.nextBytes(bytes)
    ByteString.copyFrom(bytes)
  }
}
