package pravda.node.client.impl

import com.google.protobuf.ByteString
import pravda.node.client.MetadataLanguage
import pravda.vm.Meta
import pravda.vm.asm.PravdaAssembler

import scala.concurrent.{ExecutionContext, Future}

final class MetadataLanguageImpl(implicit executionContext: ExecutionContext) extends MetadataLanguage[Future] {

  def readPrefixIncludes(source: ByteString): Future[Seq[Meta.MetaInclude]] =
    Future { PravdaAssembler.readPrefixIncludes(source) }

  def extractMeta(source: ByteString, initialShift: Int): Future[(ByteString, Map[Int, Seq[Meta]])] = Future {
    val ops = PravdaAssembler.disassemble(source)
    PravdaAssembler.assembleExtractMeta(ops.map(_._2),
                                        saveLabels = true,
                                        extractMeta = true,
                                        initialShift = initialShift)
  }

  def writePrefixIncludes(source: ByteString, includes: Seq[Meta.MetaInclude]): Future[ByteString] = Future {
    PravdaAssembler.writePrefixIncludes(source, includes)
  }
}
