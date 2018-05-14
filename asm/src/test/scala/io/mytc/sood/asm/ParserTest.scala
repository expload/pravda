package io.mytc.sood.asm

import org.scalatest._


class ParserTest extends FlatSpec with Matchers {

  "A parser" must "correctly parse sequence of ops" in {
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
        Op.Ret
      ))
    )

  }

  "A parser" must "correctly parse labels" in {
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

  "A parse" should "parse lcall op-code correctly" in {
    val p = Parser()

    p.parse("lcall Typed typedAdd 2") shouldBe Right(Seq(Op.LCall("Typed", "typedAdd", 2)))
  }
}
