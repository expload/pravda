package io.mytc.sood.forth

import org.scalatest._


class ParserTest extends FlatSpec with Matchers {

  import Statement._

  "A parser" must "correctly parse sequence of terms" in {
    val p = Parser()
    assert( p.parse("1 2 3 4 5") == Right(Seq( Integ(1), Integ(2), Integ(3), Integ(4), Integ(5) )))
    assert( p.parse("2 2 *") == Right( Seq( Integ(2), Integ(2), Ident("*")) ))
    assert( p.parse("5 6 < if print then do other") == Right(Seq(
      Integ(5), Integ(6), Ident("<"), Ident("if"), Ident("print"), Ident("then"), Ident("do"), Ident("other")
    )))
    assert( p.parse(": boo pop pop ;") == Right(Seq(
      Dword("boo", Seq( Ident("pop"), Ident("pop") ) )
    )))
  }

}
