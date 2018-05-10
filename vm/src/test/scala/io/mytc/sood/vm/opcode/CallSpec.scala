package io.mytc.sood.vm
package opcode

import org.scalatest.{FlatSpec, Matchers}
import VmUtils._
import Opcodes.int._


class CallSpec extends FlatSpec with Matchers {

  val address1 = binaryData(4, 5, 66, 78)
  val address2 = binaryData(43, 15, 66, 78)
  val address3 = binaryData(13, 15, 66, 78)


  "PCALL opcode" should "execute the program with the sepecifiedd address" in {

    val prog1 = prog.opcode(PUSHX).put(111)
    val prog2 = prog.opcode(PUSHX).put(222)
    val sum = prog.opcode(I32ADD)

    val wState = worldState(address1 -> prog1, address2 -> prog2, address3 -> sum)

    val programA1 = prog.opcode(PCALL).put(address1).put(0)
    exec(programA1, wState) shouldBe stack(data(111))
    val programA2 = prog.opcode(PUSHX).put(333).opcode(PCALL).put(address2).put(0).opcode(I32ADD)
    exec(programA2, wState) shouldBe stack(data(555))

    val programSum = prog.opcode(PUSHX).put(7).opcode(PUSHX).put(13).opcode(PCALL).put(address3).put(2)
    exec(programSum, wState) shouldBe stack(data(20))

  }
}
