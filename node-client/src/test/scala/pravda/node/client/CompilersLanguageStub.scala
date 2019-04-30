package pravda.node.client

import cats._
import com.google.protobuf.ByteString
import pravda.vm.Meta
import pravda.vm.asm.Operation

import scala.language.higherKinds

class CompilersLanguageStub[F[_]: Monad] extends CompilersLanguage[F] {

  def asm(fileName: String, source: String): F[Either[String, ByteString]] = Monad[F].pure(Right(ByteString.EMPTY))
  def asm(source: String): F[Either[String, ByteString]] = Monad[F].pure(Right(ByteString.EMPTY))
  def disasm(source: ByteString): F[String] = Monad[F].pure("")
  def disasm(source: ByteString, metas: Map[Int, Seq[Meta]]): F[String] = Monad[F].pure("")
  def disasmToOps(source: ByteString): F[Seq[(Int, Operation)]] = Monad[F].pure(Seq.empty)
  def disasmToOps(source: ByteString, metas: Map[Int, Seq[Meta]]): F[Seq[(Int, Operation)]] = Monad[F].pure(Seq.empty)

  def dotnet(sources: Seq[(ByteString, Option[ByteString])], mainClass: Option[String]): F[Either[String, ByteString]] =
    Monad[F].pure(Right(ByteString.EMPTY))
  def evm(source: ByteString, abi: ByteString): F[Either[String, ByteString]] = Monad[F].pure(Right(ByteString.EMPTY))
}
