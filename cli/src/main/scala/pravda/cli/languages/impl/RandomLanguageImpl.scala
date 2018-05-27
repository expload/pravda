package pravda.cli.languages

package impl

import java.security.SecureRandom

import com.google.protobuf.ByteString

import scala.concurrent.Future

final class RandomLanguageImpl extends RandomLanguage[Future] {

  private val secureRandom = new SecureRandom()

  def secureBytes64(): Future[ByteString] = Future.successful {
    val bytes = new Array[Byte](64)
    secureRandom.nextBytes(bytes)
    ByteString.copyFrom(bytes)
  }
}
