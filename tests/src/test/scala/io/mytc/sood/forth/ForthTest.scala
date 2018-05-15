package io.mytc.sood.forth

import com.google.protobuf.ByteString
import io.mytc.sood.vm.state.{Program, Address, Environment}
import org.scalatest._

class ForthTest extends FlatSpec with Matchers {

  import io.mytc.sood.vm.Vm
  import java.nio.ByteBuffer

  trait StackItem[T] {
    def get(item: ByteString): T
  }

  object StackItem {

    implicit val intStackItem: StackItem[Int] =
      (item: ByteString) => ByteBuffer.wrap(item.toByteArray).getInt

    implicit val floatStackItem: StackItem[Double] =
      (item: ByteString) => ByteBuffer.wrap(item.toByteArray).getDouble

  }

  def runTransaction[T](code: String)(implicit stackItem: StackItem[T]): Either[String, List[T]] = {
    Compiler().compile(code, useStdLib = true) match {
      case Left(err) ⇒ Left(err)
      case Right(code) ⇒
        val emptyState = new Environment {
          override def getProgram(address: Address): Option[Program] = None
        }
        val stack = Vm.runRaw(ByteString.copyFrom(code), ByteString.EMPTY, emptyState).stack
        Right(stack.map(stackItem.get).toList)
    }
  }

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
