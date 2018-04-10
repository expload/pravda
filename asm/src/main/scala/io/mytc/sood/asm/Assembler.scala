package io.mytc.sood.asm


class Assembler {

  def compile(code: String): Either[String, Array[Byte]] = {
    val parser = Parser()
    val bcgen = BCGen()
    parser.parse(code) match {
      case Right(ast) ⇒ Right(bcgen.gen(ast))
      case Left(err) ⇒ Left(err)
    }
  }

  def compile(code: Seq[Op]): Array[Byte] = {
    val bcgen = BCGen()
    bcgen.gen(code)
  }

}

object Assembler {
  def apply(): Assembler = new Assembler
}
