package io.mytc.sood.forth

import io.mytc.sood.vm.state.{AccountState, Address, WorldState}
import org.scalatest._


class ForthTest extends FlatSpec with Matchers {

  import io.mytc.sood.vm.Vm
  import io.mytc.sood.forth.Compiler
  import java.nio.ByteBuffer

  def run(code: String): Either[String, List[Int]] = {
    Compiler().compile(code, useStdLib=true) match {
      case Left(err)   ⇒ Left(err)
      case Right(code) ⇒ {
        val emptyState = new WorldState {
          override def get(address: Address): AccountState = ???
        }
        val stack = Vm.runTransaction(ByteBuffer.wrap(code), emptyState).stack
        Right(stack.map(_.foldLeft(0){ case (s, i) => s + i }).toList)
      }
    }
  }

  "A forth compiler" must "correctly define and run word" in {

    assert( run( """
      : seq5 1 2 3 ;
      seq5
    """ ) == Right(
      List(1, 2, 3)
    ))

  }

  "A forth program " must " be able to push to the stack" in {

    assert( run( """
      1
    """ ) == Right(
      List(1)
    ))

    assert( run( """
      1 2
    """ ) == Right(
      List(1, 2)
    ))

    assert( run( """
      1 2 3
    """ ) == Right(
      List(1, 2, 3)
    ))

  }

  "A forth standard library" must "define +" in {

    assert( run( """
      3 5 add
    """ ) == Right(
      List(8)
    ))

  }

  "A forth standard library" must "define *" in {

    assert( run( """
      1 2 3 *
    """ ) == Right(
      List(1, 6)
    ))

  }

}
