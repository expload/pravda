package io.mytc.sood.forth


class Translator {

  import io.mytc.sood.asm

  def translate(unit: Seq[Statement]): Seq[asm.Op] = {
    val ipack = unit.collect{ case s: Statement.Ident ⇒ s }.distinct.sortBy(_.v).zipWithIndex
    Seq()
  }

}
