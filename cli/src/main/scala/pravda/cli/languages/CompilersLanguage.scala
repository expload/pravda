package pravda.cli.languages

import com.google.protobuf.ByteString

import scala.language.higherKinds

trait CompilersLanguage[F[_]] {
  def asm(fileName: String, source: String): F[Either[String, ByteString]]
  def asm(source: String): F[Either[String, ByteString]]
  def disasm(source: ByteString): F[String]
  def dotnet(source: ByteString): F[Either[String, ByteString]]
}
