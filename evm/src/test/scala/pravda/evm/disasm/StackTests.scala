package pravda.evm.disasm

import utest._

object StackTests extends TestSuite {

  val tests = Tests {
    'Stack - {
      val stack = StackList.empty[Int] push 1 push 2 push 3

      stack.pop ==> Tuple2(3, StackList(2, 1))
      stack.pop._2.pop ==> Tuple2(2, StackList(1))

      stack.pop(1) ==> Tuple2(List(3), StackList(2, 1))
      stack.pop(2) ==> Tuple2(List(3, 2), StackList(1))
      stack.pop(3) ==> Tuple2(List(3, 2, 1), StackList())

      stack.swap(1) ==> StackList(2, 3, 1)
      stack.swap(2) ==> StackList(1, 2, 3)

      stack.dup(1) ==> StackList(3, 3, 2, 1)
      stack.dup(2) ==> StackList(2, 3, 2, 1)
      stack.dup(3) ==> StackList(1, 3, 2, 1)
    }
  }
}
