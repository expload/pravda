package io.mytc.sood.asm

class Assembler {

  def decompile(bcode: Array[Byte]): Seq[(Int, Op)] = {
    val bc = ByteCode()
    bc.ungen(bcode)
  }

  def compile(code: String): Either[String, Array[Byte]] = {
    val parser = Parser()
    val bc = ByteCode()
    parser.parse(code) match {
      case Right(ast) ⇒ Right(bc.gen(ast))
      case Left(err)  ⇒ Left(err)
    }
  }

  def compile(code: Seq[Op]): Array[Byte] = {
    val bc = ByteCode()
    bc.gen(code)
  }

}

object Assembler {
  def apply(): Assembler = new Assembler
}
