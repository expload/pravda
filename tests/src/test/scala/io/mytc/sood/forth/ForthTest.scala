package io.mytc.sood.forth

import io.mytc.sood.forth.ForthTestUtils._
import org.scalatest._

class ForthTest extends FlatSpec with Matchers {

  "A forth compiler" must "correctly define and run word" in {
    assert(
      runTransaction[Int](": seq5 1 2 3 ; seq5") == Right(List(1, 2, 3))
    )
  }

  "A forth program " must " be able to push to the stack" in {
    assert(
      runTransaction[Int]("1") == Right(List(1))
    )

    assert(
      runTransaction[Int]("1 2") == Right(List(1, 2))
    )

    assert(
      runTransaction[Int]("1 2 3") == Right(List(1, 2, 3))
    )
  }

  "A forth standard library" must "define +" in {
    assert(
      runTransaction[Int]("3 5 add") == Right(List(8))
    )
  }

  "A forth standard library" must "define *" in {
    assert(
      runTransaction[Int]("1 2 3 *") == Right(List(1, 6))
    )
  }
}
