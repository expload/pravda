package io.mytc.sood.forth

import org.scalatest._


class TranslatorTest extends FlatSpec with Matchers {

  import Statement._
  import io.mytc.sood.asm.Op
  import io.mytc.sood.asm.Datum

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
        Op.Push(Datum.Integral(1)),
        Op.Push(Datum.Integral(2)),
        Op.Call("add"),
        Op.Call("pushseq"),
        Op.Stop,

        Op.Label("pushseq"),
        Op.Push(Datum.Integral(1)), Op.Push(Datum.Integral(2)),
        Op.Push(Datum.Integral(3)), Op.Push(Datum.Integral(4)),
        Op.Push(Datum.Integral(5)),
        Op.Ret,

        Op.Label("seq&add"),
        Op.Push(Datum.Integral(1)), Op.Push(Datum.Integral(2)), Op.Push(Datum.Integral(3)),
        Op.Call("add"), Op.Call("add"),
        Op.Ret
      )
    )
  }

  it must "translate push" in {
    val t = Translator()
    assert(
      t.translate(Seq(
        Integ(1)
      )) ==
      Seq(
        Op.Push(Datum.Integral(1)),
        Op.Stop
      )
    )
  }

  it must "translate push large number for int32" in {
    val t = Translator()
    assert(
      t.translate(Seq(
        Integ(0xFFFFFFFF)
      )) ==
      Seq(
        Op.Push(Datum.Integral(0xFFFFFFFF)),
        Op.Stop
      )
    )
  }

  it must "translate push float" in {
    val t = Translator()
    assert(
      t.translate(Seq(
        Float(1.0)
      )) ==
      Seq(
        Op.Push(Datum.Floating(1.0)),
        Op.Stop
      )
    )
  }

  it must "translate labels" in {
    val t = Translator()
    assert(
      t.translate(Seq(
        Dword("seq", Seq(Ident("nop")))
      )) ==
      Seq(
        Op.Stop,
        Op.Label("seq"),
        Op.Call("nop"),
        Op.Ret
      )
    )
  }

}
