package pravda.node.client

import com.google.protobuf.ByteString
import pravda.vm.Meta

import scala.language.higherKinds

trait MetadataLanguage[F[_]] {
  def extractMeta(source: ByteString, initialShift: Int): F[(ByteString, Map[Int, Seq[Meta]])]
  def readPrefixIncludes(source: ByteString): F[Seq[Meta.MetaInclude]]
  def writePrefixIncludes(source: ByteString, includes: Seq[Meta.MetaInclude]): F[ByteString]
}
