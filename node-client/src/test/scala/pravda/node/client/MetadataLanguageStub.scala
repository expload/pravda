package pravda.node.client

import cats._
import com.google.protobuf.ByteString
import pravda.vm.Meta

import scala.language.higherKinds

class MetadataLanguageStub[F[_]: Monad] extends MetadataLanguage[F] {
  override def extractMeta(source: ByteString): F[(ByteString, Map[Int, Seq[Meta]])] =
    Monad[F].pure((source, Map.empty))
  override def extractPrefixIncludes(source: ByteString): F[(ByteString, Seq[Meta.MetaInclude])] =
    Monad[F].pure((source, Seq.empty))
  override def addIncludes(source: ByteString, includes: Seq[Meta.MetaInclude]): F[ByteString] = Monad[F].pure(source)
}
