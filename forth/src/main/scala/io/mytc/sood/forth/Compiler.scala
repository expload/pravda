package io.mytc.sood.forth


class Compiler {
  def compile(code: String): Either[String, Array[Byte]] = {
    import io.mytc.sood.asm.Assembler
    val parser = Parser()
    val assemb = Assembler()
    val transl = Translator()
    parser.parse(code) match {
      case Right(forthAst) ⇒ {
        val asmAst = transl.translate(forthAst)
        Right(assemb.compile(asmAst))
      }
      case Left(err) ⇒ Left(err)
    }
  }
}

object Compiler {
  def apply(): Compiler = new Compiler
}
