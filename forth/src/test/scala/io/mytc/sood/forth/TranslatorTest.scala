package io.mytc.sood.forth

import org.scalatest._


class TranslatorTest extends FlatSpec with Matchers {

  import Statement._
  import io.mytc.sood.asm.Op

  "A translator" must "correctly translate word definitions" in {
    val t = Translator()
    assert(
      t.translate(Seq(
        Integ(1),
        Integ(2),
        Ident("add"),
        Ident("pushseq"),
        Dword("pushseq", Seq(Integ(1), Integ(2), Integ(3), Integ(4), Integ(5))),
        Dword("seq&add", Seq(Integ(1), Integ(2), Integ(3), Ident("add"), Ident("add")))
      )) ==
      Seq(
        Op.Push(1),
        Op.Push(2),
        Op.Call("add"),
        Op.Call("pushseq"),
        Op.Stop,

        Op.Label("pushseq"),
        Op.Push(1), Op.Push(2), Op.Push(3), Op.Push(4), Op.Push(5),
        Op.Ret,

        Op.Label("seq&add"),
        Op.Push(1), Op.Push(2), Op.Push(3), Op.Call("add"), Op.Call("add"),
        Op.Ret
      )
    )
  }

}
