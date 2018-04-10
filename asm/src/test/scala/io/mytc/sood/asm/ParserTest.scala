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
        Op.Push(3),
        Op.Push(255),
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

}
