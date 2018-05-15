package io.mytc.sood.forth

import org.scalatest._


class ParserTest extends FlatSpec with Matchers {

  import Statement._

  "A parser" must "correctly parse sequence of terms" in {
    val p = Parser()
    assert( p.parse("1 2 3 4 5") == Right(Seq( Integ(1), Integ(2), Integ(3), Integ(4), Integ(5) )))
    assert( p.parse("2 2 *") == Right( Seq( Integ(2), Integ(2), Ident("*")) ))
    assert( p.parse(": boo pop pop ;") == Right(Seq(
      Dword("boo", Seq( Ident("pop"), Ident("pop") ) )
    )))
  }

  "A parser" must "correctly parse floats" in {
    val p = Parser()
    assert( p.parse("1.0 2.0 3.0") == Right(Seq( Float(1.0), Float(2.0), Float(3.0) )))
  }

  "A parser" must "correctly parse ifs" in {
    val p = Parser()
    assert( p.parse("if 1 2 3 then") == Right(Seq(
      If(
        Seq(
          Integ(1), Integ(2), Integ(3)
        ), Seq.empty[Statement]
      )
    )))
    assert( p.parse("a if b if c then d then") == Right(Seq(
      Ident("a"),
      If(
        Seq(
          Ident("b"),
          If(
            Seq(
              Ident("c")
            ),
            Seq()
          ),
          Ident("d")
        )
        , Seq()
      )
    )))
  }
}
