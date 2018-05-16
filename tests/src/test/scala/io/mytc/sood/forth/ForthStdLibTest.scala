package io.mytc.sood.forth

import utest._
import ForthTestUtils._


object ForthStdLibTest extends TestSuite {

  def tests = Tests {
    "dup must duplicate the top of the stack" - {

      assert( runTransaction[Int]( """
        1 2 3
        dup
      """ ) == Right(
        List(1, 2, 3, 3)
      ))

    }

    "dup1 must duplicate the top of the stack" - {

      assert( runTransaction[Int]( """
        1 2 3
        dup1
      """ ) == Right(
        List(1, 2, 3, 3)
      ))

    }

    "dup2 must push the 2nd item of the stack" - {

      assert( runTransaction[Int]( """
        1 2 3
        dup2
      """ ) == Right(
        List(1, 2, 3, 2)
      ))

    }

    "dup3 must push the 3rd item of the stack" - {

      assert( runTransaction[Int]( """
        1 2 3
        dup3
      """ ) == Right(
        List(1, 2, 3, 1)
      ))

    }

    "eq must push true if 2 top items are equal" - {

      assert( runTransaction[Boolean]( """
        1 1
        eq
      """ ) == Right(
        List(true)
      ))

    }

    "eq must push false if 2 top items are not equal" - {

      assert( runTransaction[Boolean]( """
        1 2
        eq
      """ ) == Right(
        List(false)
      ))

    }

    "neq must push false if 2 top items are equal" - {

      assert( runTransaction[Boolean]( """
        1 1
        neq
      """ ) == Right(
        List(false)
      ))

    }

    "neq must push true if 2 top items are not equal" - {

      assert( runTransaction[Boolean]( """
        1 2
        neq
      """ ) == Right(
        List(true)
      ))

    }

    "not must push !top of the stack" - {

      assert( runTransaction[Boolean]( """
        1 2
        eq
        not
      """ ) == Right(
        List(true)
      ))

    }

    "concat must take 2 arrays of bytes from top and push concatenation of them" - {

      assert( runTransaction[List[Byte]]( """
        $xFFFF
        $x0000
        concat
      """ ) == Right(
        List(List(0x00.toByte, 0x00.toByte, 0xFF.toByte, 0xFF.toByte))
      ))

    }
  }

}
