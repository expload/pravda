package io.mytc.sood.asm

import utest._


class ParserTest extends TestSuite {

  def tests = Tests {
    "A parser must correctly parse sequence of ops" - {
      val p = Parser()
      assert(
        p.parse( """
          push 3
          push 0xff
          pop
          dup
          call @boo
        @boo:
          pop
          pop
          concat
          ret
        """ ) == Right(Seq(
          Op.Push(Datum.Integral(3)),
          Op.Push(Datum.Integral(255)),
          Op.Pop,
          Op.Dup,
          Op.Call("boo"),
          Op.Label("boo"),
          Op.Pop,
          Op.Pop,
          Op.Concat,
          Op.Ret
        ))
      )

    }

    "A parser must correctly parse labels" - {
      val p = Parser()

      assert(
        p.parse( """
          @+:
        """ ) == Right(Seq(
          Op.Label("+")
        ))
      )

      assert(
        p.parse( """
          @%:
        """ ) == Right(Seq(
          Op.Label("%")
        ))
      )

      assert(
        p.parse( """
          @@:
        """ ) == Right(Seq(
          Op.Label("@")
        ))
      )
    }

    "A parser must correctly parse EQ" - {
      val p = Parser()

      assert(
        p.parse( """
          eq
          eq
        """ ) == Right(Seq(
          Op.Eq,
          Op.Eq
        ))
      )
    }

    "A parser must correctly parse FROM" - {
      val p = Parser()

      assert(
        p.parse( """
          from
        """ ) == Right(Seq(
          Op.From
        ))
      )
    }

    "A parse must parse lcall op-code correctly" - {
      val p = Parser()

      p.parse("lcall Typed typedAdd 2") ==> Right(Seq(Op.LCall("Typed", "typedAdd", 2)))
    }

    "Parser must parse pcall correctly" - {
      val p = Parser()
      p.parse("pcall $xFFFFFFFF 2") match {
        case Right(Seq(Op.PCall(addr, num))) ⇒
          assert(addr.toList == List(0xFF, 0xFF, 0xFF, 0xFF).map(_.toByte))
          assert(num == 2)
      }
    }
  }

}
