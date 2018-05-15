package io.mytc.sood.forth

import com.google.protobuf.ByteString
import io.mytc.sood.vm.state.{AccountState, Address, WorldState}
import org.scalatest._


class ForthBranchingTest extends FlatSpec with Matchers {

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

  "if" must "execute block if true is on top of the stack" in {

    assert( run[Int]( """
      0 0
      eq
      if 5 then
    """ ) == Right(
      List(5)
    ))

  }

  "if" must "not execute block if false is on top of the stack" in {

    assert( run[Int]( """
      0 1
      eq
      if 5 then
    """ ) == Right(
      List()
    ))

  }

}

