package pravda.cli.languages

import com.google.protobuf.ByteString

import scala.language.higherKinds

trait RandomLanguage[F[_]] {
  def bytes64(): F[ByteString]
}
