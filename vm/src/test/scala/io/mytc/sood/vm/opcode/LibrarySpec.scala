package io.mytc.sood.vm
package opcode

import org.scalatest.{FlatSpec, Matchers}

import VmUtils._
import Opcodes._

class LibrarySpec extends FlatSpec with Matchers {

  "Std functions " should "be accessible from a program" in {
    val program = prog
      .opcode(PUSHX).put(1)
      .opcode(PUSHX).put(7)
      .opcode(PUSHX).put(43)
    val sum2 = program
      .opcode(LCALL).put("Math").put("sum").put(2)
    val sum3 = program
      .opcode(LCALL).put("Math").put("sum").put(3)

    exec(sum2) shouldBe stack(data(1), data(50))
    exec(sum3) shouldBe stack(data(51))

  }

  "User defined functions " should "be accessible from a program" in {
    val plusLen = prog.put("plus").length
    val multLen = prog.put("mult").length

    val udflib1 = prog.opcode(FTBL)
      .put(2)
      .put("plus").put(1 + 5 + plusLen + 5 + multLen + 5)
      .put("mult").put(1 + 5 + plusLen + 5 + multLen + 5 + 1 + 1)
      .opcode(I32ADD).opcode(RET)
      .opcode(I32MUL).opcode(RET)

    val udflib2 = prog.opcode(FTBL)
      .put(1)
      .put("plus").put(1 + 5 + plusLen + 5)
      .opcode(PUSHX).put(13)
      .opcode(I32ADD)
      .opcode(I32ADD)
      .opcode(RET)


    val address1 = bytes(4, 5, 66, 78)
    val address2 = bytes(4, 6, 66, 78)

    val wState = worldState(address1 -> udflib1, address2 -> udflib2)

    val program = prog
      .opcode(PUSHX).put(7)
      .opcode(PUSHX).put(8)

    val plus1 = program.opcode(LCALL).put(address1).put("plus").put(2)
    val plus2 = program.opcode(LCALL).put(address2).put("plus").put(2)
    val mult1 = program.opcode(LCALL).put(address1).put("mult").put(2)

    exec(plus1, wState) shouldBe stack(data(15))
    exec(plus2, wState) shouldBe stack(data(28))
    exec(mult1, wState) shouldBe stack(data(56))

  }
}
