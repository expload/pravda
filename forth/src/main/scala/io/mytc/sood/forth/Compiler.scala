package io.mytc.sood.forth


class Compiler {
  def compile(code: String, useStdLib: Boolean = false): Either[String, Array[Byte]] = {
    import io.mytc.sood.asm.Assembler
    val parser = Parser()
    val assemb = Assembler()
    val transl = Translator()
    val prog = if (useStdLib) StdLib.defs ++ code else code
    parser.parse(prog) match {
      case Right(forthAst) ⇒ {
        val asmAst = transl.translate(forthAst, useStdLib)
        Right(assemb.compile(asmAst))
      }
      case Left(err) ⇒ Left(err)
    }
  }
}

object Compiler {
  def apply(): Compiler = new Compiler
}
