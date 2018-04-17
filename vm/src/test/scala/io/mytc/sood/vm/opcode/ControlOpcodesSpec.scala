package io.mytc.sood.vm
package opcode

import VmUtils._
import io.mytc.sood.vm.Opcodes.int._
import org.scalatest.{FlatSpec, Matchers}

class ControlOpcodesSpec extends FlatSpec with Matchers {

  "CALL and RET opcodes" should "jump, remember where we have been before the jump and move there" in {
    val program = prog
      .opcode(PUSHX).put(14)
      .opcode(JUMP) // jump over the procedure
      .opcode(PUSHX).put(325).opcode(RET) // procedure
    val program1 = program
      .opcode(PUSHX).put(7).opcode(CALL) // call procedure
    val program2 = program
      .opcode(PUSHX).put(7).opcode(CALL) // call 1
      .opcode(PUSHX).put(7).opcode(CALL) // call 2

    exec(program) shouldBe empty
    exec(program1) shouldBe stack(data(325))
    exec(program2) shouldBe stack(data(325), data(325))
  }

  "STOP opcode" should "stop the program" in {
    val regularProgram = prog
      .opcode(PUSHX).put(31)
      .opcode(PUSHX).put(54)
    exec(regularProgram) shouldBe stack(data(31), data(54))

    val programWithDeadCode = prog
      .opcode(PUSHX).put(31)
      .opcode(STOP)
      .opcode(PUSHX).put(54) // DEAD code
    exec(programWithDeadCode) shouldBe stack(data(31))
  }

  "JUMP opcode" should "do a jump" in {
    val regularProgram = prog
      .opcode(PUSHX).put(31)
      .opcode(PUSHX).put(54)
    exec(regularProgram) shouldBe stack(data(31), data(54))

    val programWithJump = prog
        .opcode(PUSHX) // 0
        .put(13) // 1
        .opcode(JUMP) // 6
        .opcode(PUSHX).put(31) // 7, 8
        .opcode(PUSHX).put(54) // 13, 14
    exec(programWithJump) shouldBe stack(data(54))
  }

  "JUMPI opcode" should "do a conditional jump" in {
    def iprogram(i: Int) = prog
      .opcode(PUSHX) // 0
      .put(19) // 1
      .opcode(PUSHX) // 6
      .put(i) // 7
      .opcode(JUMPI) // 12
      .opcode(PUSHX).put(31) // 13, 14
      .opcode(PUSHX).put(54) // 19, 20

    def bprogram(b: Byte) = prog
      .opcode(PUSHX) // 0
      .put(15) // 1
      .opcode(PUSHX) // 6
      .put(b) // 7
      .opcode(JUMPI) // 8
      .opcode(PUSHX).put(31) // 9, 10
      .opcode(PUSHX).put(54) // 15, 16


    exec(iprogram(1)) shouldBe stack(data(54))
    exec(iprogram(2)) shouldBe stack(data(54))
    exec(iprogram(0)) shouldBe stack(data(31), data(54))
    exec(iprogram(-1)) shouldBe stack(data(31), data(54))


    exec(bprogram(1)) shouldBe stack(data(54))
    exec(bprogram(2)) shouldBe stack(data(54))
    exec(bprogram(0)) shouldBe stack(data(31), data(54))

  }

}
