package pravda.cli.languages

package impl

import java.security.SecureRandom

import cats.Id
import com.google.protobuf.ByteString

final class RandomLanguageImpl extends RandomLanguage[Id] {

  private val secureRandom = new SecureRandom()

  def secureBytes64(): Id[ByteString] = {
    val bytes = new Array[Byte](64)
    secureRandom.nextBytes(bytes)
    ByteString.copyFrom(bytes)
  }
}
