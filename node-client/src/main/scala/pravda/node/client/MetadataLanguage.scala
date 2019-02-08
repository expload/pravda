package pravda.node.client

import com.google.protobuf.ByteString
import pravda.vm.Meta

import scala.language.higherKinds

trait MetadataLanguage[F[_]] {
  def extractMeta(source: ByteString): F[(ByteString, Map[Int, Seq[Meta]])]
  def extractPrefixIncludes(source: ByteString): F[(ByteString, Seq[Meta.MetaInclude])]
  def addIncludes(source: ByteString, includes: Seq[Meta.MetaInclude]): F[ByteString]
}
