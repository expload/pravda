package pravda.vm
package opcode

import VmUtils._
import Opcodes.int._

import utest._

object CallTests extends TestSuite {

  val address1 = binaryData(4, 5, 66, 78)
  val address2 = binaryData(43, 15, 66, 78)
  val address3 = binaryData(13, 15, 66, 78)


  val tests = Tests {
    'pcall - {

      val prog1 = prog.opcode(PUSHX).put(111)
      val prog2 = prog.opcode(PUSHX).put(222)
      val sum = prog.opcode(I32ADD)

      val wState = environment(address1 -> prog1, address2 -> prog2, address3 -> sum)

      val programA1 = prog.opcode(PUSHX).put(address1).opcode(PUSHX).put(0).opcode(PCALL)
      exec(programA1, wState) ==> stack(data(111))
      val programA2 = prog.opcode(PUSHX).put(333).opcode(PUSHX).put(address2).opcode(PUSHX).put(0).opcode(PCALL).opcode(I32ADD)
      exec(programA2, wState) ==> stack(data(555))

      val programSum = prog.opcode(PUSHX).put(7).opcode(PUSHX).put(13).opcode(PUSHX).put(address3).opcode(PUSHX).put(2).opcode(PCALL)
      exec(programSum, wState) ==> stack(data(20))

    }
  }
}
