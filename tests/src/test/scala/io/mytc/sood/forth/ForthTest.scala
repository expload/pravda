package io.mytc.sood.forth

import io.mytc.sood.vm.state.{AccountState, Address, WorldState}
import org.scalatest._
import scodec.bits.ByteVector


class ForthTest extends FlatSpec with Matchers {

  import io.mytc.sood.vm.Vm
  import io.mytc.sood.forth.Compiler
  import java.nio.ByteBuffer

  trait StackItem[T] {
    def get(item: ByteVector): T
  }

  object StackItem {

    implicit val intStackItem: StackItem[Int] = new StackItem[Int] {
      def get(item: ByteVector): Int = item.foldLeft(0){ case (s, i) => s + i }
    }

    implicit val floatStackItem: StackItem[Double] = new StackItem[Double] {
      def get(item: ByteVector): Double = item.toByteBuffer.getDouble
    }

  }

  def run[T](code: String)(implicit stackItem: StackItem[T]): Either[String, List[T]] = {
    Compiler().compile(code, useStdLib=true) match {
      case Left(err)   ⇒ Left(err)
      case Right(code) ⇒ {
        val emptyState = new WorldState {
          override def get(address: Address): Option[AccountState] = None
        }
        val stack = Vm.runTransaction(ByteBuffer.wrap(code), emptyState).stack
        Right(stack.map(_.foldLeft(0){ case (s, i) => s + i }).toList)
        Right(stack.map(stackItem.get).toList)
      }
    }
  }

  "A forth compiler" must "correctly define and run word" in {

    assert( run[Int]( """
      : seq5 1 2 3 ;
      seq5
    """ ) == Right(
      List(1, 2, 3)
    ))

  }

  "A forth program " must " be able to push to the stack" in {

    assert( run[Int]( """
      1
    """ ) == Right(
      List(1)
    ))

    assert( run[Int]( """
      1 2
    """ ) == Right(
      List(1, 2)
    ))

    assert( run[Int]( """
      1 2 3
    """ ) == Right(
      List(1, 2, 3)
    ))

  }

  "A forth standard library" must "define +" in {

    assert( run[Int]( """
      3 5 add
    """ ) == Right(
      List(8)
    ))

  }

  "A forth standard library" must "define *" in {

    assert( run[Int]( """
      1 2 3 *
    """ ) == Right(
      List(1, 6)
    ))

  }

}
