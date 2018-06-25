package pravda.cli.languages

import com.google.protobuf.ByteString

import scala.language.higherKinds

trait CompilersLanguage[F[_]] {
  def asm(source: String): F[Either[String, ByteString]]
  def disasm(source: ByteString): F[String]
  def forth(source: String): F[Either[String, ByteString]]
  def dotnet(source: ByteString): F[Either[String, ByteString]]
  def disnet(source: ByteString): F[Either[String, String]]
}
