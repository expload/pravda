package io.mytc.sood.forth

import utest._


object TranslatorTest extends TestSuite {

  import Statement._
  import io.mytc.sood.asm.Op
  import io.mytc.sood.asm.Datum

  def tests = Tests {
    "A translator must correctly translate word definitions" - {
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

    "Translator must translate push" - {
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

    "Translator must translate push large number for int32" - {
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

    "Translator must translate push float" - {
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

    "Translator must translate labels" - {
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

    "Translator must translate ifs" - {
      val t = Translator()
      assert(
        t.translate(Seq(
          Dword("seq", Seq(Ident("nop"))),
          If(pos = Seq(Ident("nop"), Ident("nop")), neg = Seq())
        )) ==
        Seq(
          Op.Not,
          Op.JumpI(t.mangleIf(t.mainName, 0)),
          Op.Call("nop"),
          Op.Call("nop"),
          Op.Label(t.mangleIf(t.mainName, 0)),
          Op.Stop,
          Op.Label("seq"),
          Op.Call("nop"),
          Op.Ret
        )
      )
    }
  }

}
