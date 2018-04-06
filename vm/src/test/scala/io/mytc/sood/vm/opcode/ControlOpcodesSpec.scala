package io.mytc.sood.vm
package opcode

import VmUtils._
import io.mytc.sood.vm.Opcodes.int._
import org.scalatest.{FlatSpec, Matchers}

class ControlOpcodesSpec extends FlatSpec with Matchers {

  "CALL and RET opcodes" should "jump, remember where we have been before the jump and move there" in {
    val program = prog.withStack(int32ToWord(7))
      .opcode(JUMP) // jump over the procedure
      .opcode(PUSH4).put(325).opcode(RET) // procedure
    val program1 = program
      .opcode(PUSH4).put(1).opcode(CALL) // call procedure
    val program2 = program
      .opcode(PUSH4).put(1).opcode(CALL) // call 1
      .opcode(PUSH4).put(1).opcode(CALL) // call 2

    exec(program) shouldBe empty
    exec(program1) shouldBe stack(int32ToWord(325))
    exec(program2) shouldBe stack(int32ToWord(325), int32ToWord(325))
  }

  "STOP opcode" should "stop the program" in {
    val regularProgram = prog
      .opcode(PUSH4).put(31)
      .opcode(PUSH4).put(54)
    exec(regularProgram) shouldBe stack(int32ToWord(31), int32ToWord(54))

    val programWithDeadCode = prog
      .opcode(PUSH4).put(31)
      .opcode(STOP)
      .opcode(PUSH4).put(54) // DEAD code
    exec(programWithDeadCode) shouldBe stack(int32ToWord(31))
  }

  "JUMP opcode" should "do a jump" in {
    val regularProgram = prog
      .opcode(PUSH4).put(31)
      .opcode(PUSH4).put(54)
    exec(regularProgram) shouldBe stack(int32ToWord(31), int32ToWord(54))

    val programWithJump = prog
        .opcode(PUSH4) // 0
        .put(11) // 1
        .opcode(JUMP) // 5
        .opcode(PUSH4).put(31) // 6, 7
        .opcode(PUSH4).put(54) // 11, 12
    exec(programWithJump) shouldBe stack(int32ToWord(54))
  }

  "JUMPI opcode" should "do a conditional jump" in {
    def iprogram(i: Int) = prog
      .opcode(PUSH4) // 0
      .put(16) // 1
      .opcode(PUSH4) // 5
      .put(i) // 6
      .opcode(JUMPI) // 10
      .opcode(PUSH4).put(31) // 11, 12
      .opcode(PUSH4).put(54) // 16, 17

    def bprogram(b: Byte) = prog
      .opcode(PUSH4) // 0
      .put(13) // 1
      .opcode(PUSH1) // 5
      .put(b) // 6
      .opcode(JUMPI) // 7
      .opcode(PUSH4).put(31) // 8, 9
      .opcode(PUSH4).put(54) // 13, 14


    exec(iprogram(1)) shouldBe stack(int32ToWord(54))
    exec(iprogram(2)) shouldBe stack(int32ToWord(54))
    exec(iprogram(0)) shouldBe stack(int32ToWord(31), int32ToWord(54))
    exec(iprogram(-1)) shouldBe stack(int32ToWord(31), int32ToWord(54))


    exec(bprogram(1)) shouldBe stack(int32ToWord(54))
    exec(bprogram(2)) shouldBe stack(int32ToWord(54))
    exec(bprogram(0)) shouldBe stack(int32ToWord(31), int32ToWord(54))
    exec(bprogram(-1)) shouldBe stack(int32ToWord(31), int32ToWord(54))

  }

}
