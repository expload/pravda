package pravda.vm
package opcode

import VmUtils._
import Opcodes.int._

import utest._

object CallTests extends TestSuite {

  val address1 = data(4.toByte, 5.toByte, 66.toByte, 78.toByte)
  val address2 = data(43.toByte, 15.toByte, 66.toByte, 78.toByte)
  val address3 = data(13.toByte, 15.toByte, 66.toByte, 78.toByte)


  val tests = Tests {
    'pcall - {

      val prog1 = prog.opcode(PUSHX).put(111)
      val prog2 = prog.opcode(PUSHX).put(222)
      val sum = prog.opcode(I32ADD)

      val wState = environment(address1 -> prog1, address2 -> prog2, address3 -> sum)

      val programA1 = prog.opcode(PUSHX).put(address1).opcode(PUSHX).put(0).opcode(PCALL)
      exec(programA1, wState) ==> stack(int2Data(111))
      val programA2 = prog.opcode(PUSHX).put(333).opcode(PUSHX).put(address2).opcode(PUSHX).put(0).opcode(PCALL).opcode(I32ADD)
      exec(programA2, wState) ==> stack(int2Data(555))

      val programSum = prog.opcode(PUSHX).put(7).opcode(PUSHX).put(13).opcode(PUSHX).put(address3).opcode(PUSHX).put(2).opcode(PCALL)
      exec(programSum, wState) ==> stack(int2Data(20))

    }
  }
}
