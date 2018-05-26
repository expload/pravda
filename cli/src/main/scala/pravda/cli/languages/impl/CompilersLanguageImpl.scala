package pravda.cli.languages

package impl

import cats.Id
import com.google.protobuf.ByteString
import pravda.forth.{Compiler => ForthCompiler}
import pravda.vm.asm.Assembler

final class CompilersLanguageImpl extends CompilersLanguage[Id] {

  def asm(source: String): Id[Either[String, ByteString]] =
    Assembler()
      .compile(source)
      .map(a => ByteString.copyFrom(a))

  def disasm(source: ByteString): Id[String] =
    Assembler()
      .decompile(source)
      .map { case (no, op) => "%06X:\t%s".format(no, op.toAsm) }
      .mkString("\n")

  def forth(source: String): Id[Either[String, ByteString]] =
    ForthCompiler().compileToByteString(source)
}
