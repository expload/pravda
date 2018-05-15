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

  "A parser" must "correctly parse EQ" in {
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

  "A parser" must "correctly parse FROM" in {
    val p = Parser()

    assert(
      p.parse( """
        from
      """ ) == Right(Seq(
        Op.From
      ))
    )
  }

  "A parse" should "parse lcall op-code correctly" in {
    val p = Parser()

    p.parse("lcall Typed typedAdd 2") shouldBe Right(Seq(Op.LCall("Typed", "typedAdd", 2)))
  }

  "Parser" should "parse pcall correctly" in {
    val p = Parser()
    p.parse("pcall $xFFFFFFFF 2") match {
      case Right(Seq(Op.PCall(addr, num))) ⇒
        assert(addr.toList == List(0xFF, 0xFF, 0xFF, 0xFF).map(_.toByte))
        assert(num == 2)
    }
  }

}
