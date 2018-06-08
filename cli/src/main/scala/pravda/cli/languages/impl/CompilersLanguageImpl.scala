package pravda.cli.languages

package impl

import com.google.protobuf.ByteString
import pravda.forth.{Compiler => ForthCompiler}
import pravda.vm.asm.Assembler
import pravda.dotnet.translation.{Translator => DotnetTranslator, TranslationVisualizer}
import pravda.dotnet.parsers.{FileParser => DotnetParser}

import scala.concurrent.{ExecutionContext, Future}

final class CompilersLanguageImpl(implicit executionContext: ExecutionContext) extends CompilersLanguage[Future] {

  def asm(source: String): Future[Either[String, ByteString]] = Future {
    Assembler()
      .compile(source)
      .map(a => ByteString.copyFrom(a))
  }

  def disasm(source: ByteString): Future[String] = Future {
    Assembler()
      .decompile(source)
      .map { case (no, op) => "%06X:\t%s".format(no, op.toAsm) }
      .mkString("\n")
  }

  def forth(source: String): Future[Either[String, ByteString]] = Future {
    ForthCompiler().compileToByteString(source)
  }

  def dotnet(source: ByteString): Future[Either[String, ByteString]] = Future {
    for {
       pe <- DotnetParser.parsePe(source.toByteArray)
       (_, cilData, methods, signatures) = pe
      ops <- DotnetTranslator.translateAsm(methods, cilData, signatures)
    } yield ByteString.copyFrom(Assembler().compile(ops))
  }

  def disnet(source: ByteString): Future[Either[String, String]] = Future {
    for {
      pe <- DotnetParser.parsePe(source.toByteArray)
      (_, cilData, methods, signatures) = pe
      t <- DotnetTranslator.translateVerbose(methods, cilData, signatures)
    } yield TranslationVisualizer.visualize(t)
  }
}
