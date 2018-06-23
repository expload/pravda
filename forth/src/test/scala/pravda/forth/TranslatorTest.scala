package pravda.forth

import utest._


object TranslatorTest extends TestSuite {

  import Statement._
  import pravda.vm.asm.Operation
  import pravda.vm.asm.Datum

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
          Operation.Push(Datum.Integral(1)),
          Operation.Push(Datum.Integral(2)),
          Operation.Call("add"),
          Operation.Call("pushseq"),
          Operation.Stop,

          Operation.Label("pushseq"),
          Operation.Push(Datum.Integral(1)), Operation.Push(Datum.Integral(2)),
          Operation.Push(Datum.Integral(3)), Operation.Push(Datum.Integral(4)),
          Operation.Push(Datum.Integral(5)),
          Operation.Ret,

          Operation.Label("seq&add"),
          Operation.Push(Datum.Integral(1)), Operation.Push(Datum.Integral(2)), Operation.Push(Datum.Integral(3)),
          Operation.Call("add"), Operation.Call("add"),
          Operation.Ret
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
          Operation.Push(Datum.Integral(1)),
          Operation.Stop
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
          Operation.Push(Datum.Integral(0xFFFFFFFF)),
          Operation.Stop
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
          Operation.Push(Datum.Floating(1.0)),
          Operation.Stop
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
          Operation.Stop,
          Operation.Label("seq"),
          Operation.Call("nop"),
          Operation.Ret
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
          Operation.Not,
          Operation.JumpI(t.mangleIf(t.mainName, 0)),
          Operation.Call("nop"),
          Operation.Call("nop"),
          Operation.Label(t.mangleIf(t.mainName, 0)),
          Operation.Stop,
          Operation.Label("seq"),
          Operation.Call("nop"),
          Operation.Ret
        )
      )
    }
  }

}
