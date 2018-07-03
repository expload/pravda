package pravda.cli.languages

package impl

import com.google.protobuf.ByteString
import pravda.dotnet.parsers.{FileParser => DotnetParser}
import pravda.dotnet.translation.{Translator => DotnetTranslator}
import pravda.vm.asm.PravdaAssembler

import scala.concurrent.{ExecutionContext, Future}

final class CompilersLanguageImpl(implicit executionContext: ExecutionContext) extends CompilersLanguage[Future] {

  def asm(fileName: String, source: String): Future[Either[String, ByteString]] = Future {
    PravdaAssembler.assemble(source, saveLabels = true).left.map(s => s"$fileName:${s.mkString}")
  }

  def asm(source: String): Future[Either[String, ByteString]] = Future {
    PravdaAssembler.assemble(source, saveLabels = true).left.map(_.mkString)
  }

  def disasm(source: ByteString): Future[String] = Future {
    PravdaAssembler.render(PravdaAssembler.disassemble(source))
  }

  def dotnet(source: ByteString): Future[Either[String, ByteString]] = Future {
    for {
      pe <- DotnetParser.parsePe(source.toByteArray)
      (_, cilData, methods, signatures) = pe
      ops <- DotnetTranslator.translateAsm(methods, cilData, signatures)
      code <- Right(PravdaAssembler.assemble(ops, saveLabels = true))
    } yield code
  }
}
