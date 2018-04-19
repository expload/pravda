package io.mytc.sood.forth

class Translator {

  import io.mytc.sood.asm.Op

  // TODO: name mangling
  def mangle(name: String): String = name

  def stmts(unit: Seq[Statement]): Seq[Statement] = {
    unit.filter(!_.isInstanceOf[Statement.Dword])
  }

  def words(unit: Seq[Statement]): Seq[Statement.Dword] = {
    unit.collect { case v: Statement.Dword ⇒ v.copy(name = mangle(v.name)) }
  }

  def translateStmts(stmts: Seq[Statement]): Seq[Op] = {
    stmts.map { w ⇒
      w match {
        case Statement.Ident(n) ⇒ Op.Call(mangle(n))
        case Statement.Integ(v) ⇒ Op.Push(v)
        case _                  ⇒ Op.Nop
      }
    }
  }

  def translateWords(words: Seq[Statement.Dword]): Seq[Op] = {
    words.map { w ⇒
      Op.Label(name = w.name) +: translateStmts(w.block) :+ Op.Ret
    }.flatten
  }

  def translate(unit: Seq[Statement], useStdLib: Boolean = false): Seq[Op] = {
    val mainUnit = translateStmts(stmts(unit)) ++ Seq(Op.Stop) ++ translateWords(words(unit))
    if (useStdLib) {
      mainUnit ++ StdLib.words
    } else {
      mainUnit
    }
  }

}

object Translator {
  def apply(): Translator = new Translator
}
