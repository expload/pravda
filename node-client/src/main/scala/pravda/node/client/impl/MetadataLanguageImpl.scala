package pravda.node.client.impl
import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.node.client.MetadataLanguage
import pravda.vm.{Meta, Opcodes}
import pravda.vm.asm.PravdaAssembler

import scala.concurrent.{ExecutionContext, Future}

final class MetadataLanguageImpl(implicit executionContext: ExecutionContext) extends MetadataLanguage[Future] {

  def extractPrefixIncludes(source: ByteString): Future[(ByteString, Seq[Meta.MetaInclude])] =
    Future { PravdaAssembler.extractPrefixIncludes(source) }

  def extractMeta(source: ByteString): Future[(ByteString, Map[Int, Seq[Meta]])] = Future {
    val ops = PravdaAssembler.disassemble(source)
    PravdaAssembler.assembleExtractMeta(ops.map(_._2), saveLabels = true, extractMeta = true)
  }

  def addPrefixIncludes(source: ByteString, includes: Seq[Meta.MetaInclude]): Future[ByteString] = Future {
    val buffer = ByteBuffer.allocate(1024 * 1024)
    includes.foreach { i =>
      buffer.put(Opcodes.META.toByte)
      i.writeToByteBuffer(buffer)
    }
    buffer.put(source.toByteArray)

    buffer.flip()
    ByteString.copyFrom(buffer)
  }
}
