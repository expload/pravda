package io.mytc.sood.forth

import com.google.protobuf.ByteString
import io.mytc.sood.vm.state._
import org.scalatest._

import scala.collection.mutable

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
        val emptyState = new WorldState {
          override def get(address: Address): Option[AccountState] = None
        }
        val stack = Vm.runTransaction(ByteBuffer.wrap(code), emptyState).stack
        Right(stack.map(stackItem.get).toList)
    }
  }

  def runProgram[T](code: String)(implicit stackItem: StackItem[T]): Unit = {
    val programAddress = ByteString.copyFrom(Array[Byte](1, 2, 3))

    Compiler().compile(code, useStdLib = true) match {
      case Left(err) ⇒ Left(err)
      case Right(code) ⇒
        val programStorageMap = mutable.Map[Address, Data]()
        val programStorage = new Storage {
          override def get(key: Address): Option[Data] = programStorageMap.get(key)
          override def put(key: Address, value: Data): Unit = programStorageMap.put(key, value)
          override def delete(key: Address): Unit = programStorageMap.remove(key)
        }

        val stateWithAccount = new WorldState {
          override def get(address: Address): Option[AccountState] =
            if (address == programAddress) {
              Some(new AccountState {
                override def storage: Storage = programStorage
                override def program: ByteBuffer = ByteBuffer.wrap(code)
              })
            } else {
              None
            }
        }
        val stack = Vm.runProgram(programAddress, Memory.empty, stateWithAccount).stack
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

  "Smart-program" should "run correctly" in {
    runProgram[Int](
      """
        |loadData
        |dup3 1 == if dup1 sget dup3 + dup2 sput then
        |dup3 2 == if dup1 sget dup3 - dup2 sput then
      """.stripMargin
    ) shouldBe List()
  }

}
