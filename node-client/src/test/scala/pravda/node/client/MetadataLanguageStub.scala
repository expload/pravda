package pravda.node.client

import cats._
import com.google.protobuf.ByteString
import pravda.vm.Meta

import scala.language.higherKinds

class MetadataLanguageStub[F[_]: Monad] extends MetadataLanguage[F] {

  def extractMeta(source: ByteString, initialShift: Int): F[(ByteString, Map[Int, Seq[Meta]])] =
    Monad[F].pure((source, Map.empty))

  def readPrefixIncludes(source: ByteString): F[Seq[Meta.MetaInclude]] =
    Monad[F].pure(Seq.empty)
  def writePrefixIncludes(source: ByteString, includes: Seq[Meta.MetaInclude]): F[ByteString] = Monad[F].pure(source)
}
