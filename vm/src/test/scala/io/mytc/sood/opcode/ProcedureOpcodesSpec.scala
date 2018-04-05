package io.mytc.sood
package opcode

import org.scalatest.{FlatSpec, Matchers}
import serialize._
import vm.Vm._

class ProcedureOpcodesSpec extends FlatSpec with Matchers {

  "CALL and RET opcodes" should "jump, remember where we have been before the jump and move there" in {
    val program = prog.withStack(pureWord(8))
      .opcode(JUMP) // jump over the procedure
      .opcode(PUSHX).put(325).opcode(RET) // procedure
    val program1 = program
      .opcode(PUSHX).put(1).opcode(CALL) // call procedure
    val program2 = program
      .opcode(PUSHX).put(1).opcode(CALL) // call 1
      .opcode(PUSHX).put(1).opcode(CALL) // call 2

    exec(program) shouldBe empty
    exec(program1) shouldBe stack(pureWord(325))
    exec(program2) shouldBe stack(pureWord(325), pureWord(325))
  }
}
