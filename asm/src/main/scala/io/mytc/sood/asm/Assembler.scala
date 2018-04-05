package io.mytc.sood.asm


class Assembler {

  private val parser = Parser()
  private val bcgen = BCGen()

  def compile(code: String): Either[String, Array[Byte]] = {
    parser.parse(code) match {
      case Right(ast) ⇒ Right(bcgen.gen(ast))
      case Left(err) ⇒ Left(err)
    }
  }

}

object Assembler {
  def apply(): Assembler = new Assembler
}
