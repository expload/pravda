package io.mytc.sood.forth

import com.google.protobuf.ByteString

class Compiler {

  def compile(code: String, useStdLib: Boolean = true): Either[String, Array[Byte]] = {
    import pravda.vm.asm.Assembler
    val parser = Parser()
    val assemb = Assembler()
    val transl = Translator()
    val prog = if (useStdLib) StdLib.defs ++ code else code
    parser.parse(prog) match {
      case Right(forthAst) ⇒ {
        scala.util
          .Try {
            val asmAst = transl.translate(forthAst, useStdLib)
            assemb.compile(asmAst)
          }
          .map(Right(_))
          .recover { case err => Left(err.getMessage) }
          .get
      }
      case Left(err) ⇒ Left(err)
    }
  }

  def compileToByteString(code: String, useStdLib: Boolean = true): Either[String, ByteString] = {
    compile(code, useStdLib).map(ByteString.copyFrom(_))
  }

}

object Compiler {
  def apply(): Compiler = new Compiler
}
