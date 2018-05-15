package io.mytc.sood.forth

import com.google.protobuf.ByteString
import io.mytc.sood.vm.state.{AccountState, Address, WorldState}
import org.scalatest._


class ForthStdLibTest extends FlatSpec with Matchers {

  import io.mytc.sood.vm.Vm
  import io.mytc.sood.forth.Compiler
  import java.nio.ByteBuffer

  trait StackItem[T] {
    def get(item: ByteString): T
  }

  object StackItem {

    implicit val intStackItem: StackItem[Int] =
      (item: ByteString) => ByteBuffer.wrap(item.toByteArray).getInt

    implicit val floatStackItem: StackItem[Double] =
      (item: ByteString) => ByteBuffer.wrap(item.toByteArray).getDouble

    implicit val boolStackItem: StackItem[Boolean] =
      (item: ByteString) => if ((ByteBuffer.wrap(item.toByteArray).get & 0xFF) == 1) true else false

  }

  def run[T](code: String)(implicit stackItem: StackItem[T]): Either[String, List[T]] = {
    Compiler().compile(code, useStdLib=true) match {
      case Left(err)   ⇒ Left(err)
      case Right(code) ⇒
        val emptyState = new WorldState {
          override def get(address: Address): Option[AccountState] = None
        }
        val stack = Vm.runTransaction(ByteBuffer.wrap(code), emptyState).stack
        Right(stack.map(stackItem.get).toList)
    }
  }

  "dup" must "duplicate the top of the stack" in {

    assert( run[Int]( """
      1 2 3
      dup
    """ ) == Right(
      List(1, 2, 3, 3)
    ))

  }

  "dup1" must "duplicate the top of the stack" in {

    assert( run[Int]( """
      1 2 3
      dup1
    """ ) == Right(
      List(1, 2, 3, 3)
    ))

  }

  "dup2" must "push the 2-nd item of the stack" in {

    assert( run[Int]( """
      1 2 3
      dup2
    """ ) == Right(
      List(1, 2, 3, 2)
    ))

  }

  "dup3" must "push the 3-nd item of the stack" in {

    assert( run[Int]( """
      1 2 3
      dup3
    """ ) == Right(
      List(1, 2, 3, 1)
    ))

  }

  "eq" must "push true if 2 top items are equal" in {

    assert( run[Boolean]( """
      1 1
      eq
    """ ) == Right(
      List(true)
    ))

  }

  "eq" must "push false if 2 top items are not equal" in {

    assert( run[Boolean]( """
      1 2
      eq
    """ ) == Right(
      List(false)
    ))

  }

  "neq" must "push false if 2 top items are equal" in {

    assert( run[Boolean]( """
      1 1
      neq
    """ ) == Right(
      List(false)
    ))

  }

  "neq" must "push true if 2 top items are not equal" in {

    assert( run[Boolean]( """
      1 2
      neq
    """ ) == Right(
      List(true)
    ))

  }

}
