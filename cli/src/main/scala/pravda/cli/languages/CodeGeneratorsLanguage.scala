package pravda.cli.languages

import com.google.protobuf.ByteString

import scala.language.higherKinds

trait CodeGeneratorsLanguage[F[_]] {
  def dotnet(input: ByteString, excludeBigInteger: Boolean): F[List[(String, String)]]
}
