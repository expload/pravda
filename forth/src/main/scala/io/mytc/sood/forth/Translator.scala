package io.mytc.sood.forth

import java.nio.charset.StandardCharsets

class Translator {

  import pravda.vm.asm.Op
  import pravda.vm.asm.Datum

  val mainName = "__main__"

  // TODO: name mangling
  def mangle(name: String): String = name
  def mangleIf(name: String, idx: Int): String = s"__if${name}_${idx}"

  def stmts(unit: Seq[Statement]): Seq[Statement] = {
    unit.filter(!_.isInstanceOf[Statement.Dword])
  }

  def words(unit: Seq[Statement]): Seq[Statement.Dword] = {
    unit.collect {
      case v: Statement.Dword ⇒ v.copy(name = mangle(v.name))
    }
  }

  def translateStmts(stmts: Seq[Statement], prefix: String): Seq[Op] = {
    stmts.zipWithIndex.flatMap { w ⇒
      w match {
        case (Statement.Ident(n), i) ⇒ List(Op.Call(mangle(n)))
        case (Statement.Integ(v), i) ⇒ List(Op.Push(Datum.Integral(v)))
        case (Statement.Float(v), i) ⇒ List(Op.Push(Datum.Floating(v)))
        case (Statement.Hexar(v), i) ⇒ List(Op.Push(Datum.Rawbytes(v)))
        case (Statement.If(p, n), i) ⇒
          List(
            List(Op.Not),
            List(Op.JumpI(mangleIf(prefix, i))),
            translateStmts(p, prefix + prefix),
            List(Op.Label(mangleIf(prefix, i)))
          ).flatten
        case (Statement.Chrar(s), i) => List(Op.Push(Datum.Rawbytes(s.getBytes(StandardCharsets.UTF_8))))
        case _                       ⇒ List(Op.Nop)
      }
    }
  }

  def translateWords(words: Seq[Statement.Dword]): Seq[Op] = {
    words.map { w ⇒
      Op.Label(name = w.name) +: translateStmts(w.block, w.name) :+ Op.Ret
    }.flatten
  }

  def translate(unit: Seq[Statement], useStdLib: Boolean = false): Seq[Op] = {
    val mainUnit = translateStmts(stmts(unit), mainName) ++ Seq(Op.Stop) ++ translateWords(words(unit))
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
