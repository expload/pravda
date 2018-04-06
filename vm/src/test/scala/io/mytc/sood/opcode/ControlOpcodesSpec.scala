package io.mytc.sood
package opcode

import serialize._
import org.scalatest.{FlatSpec, Matchers}
import vm.Opcodes.int._

class ControlOpcodesSpec extends FlatSpec with Matchers {

  "STOP opcode" should "stop the program" in {
    val regularProgram = prog
      .opcode(PUSH4).put(31)
      .opcode(PUSH4).put(54)
    exec(regularProgram) shouldBe stack(pureWord(31), pureWord(54))

    val programWithDeadCode = prog
      .opcode(PUSH4).put(31)
      .opcode(STOP)
      .opcode(PUSH4).put(54) // DEAD code
    exec(programWithDeadCode) shouldBe stack(pureWord(31))
  }

  "JUMP opcode" should "do a jump" in {
    val regularProgram = prog
      .opcode(PUSH4).put(31)
      .opcode(PUSH4).put(54)
    exec(regularProgram) shouldBe stack(pureWord(31), pureWord(54))

    val programWithJump = prog
        .opcode(PUSH4) // 0
        .put(13) // 1
        .opcode(JUMP) // 6
        .opcode(PUSH4).put(31) // 7, 8
        .opcode(PUSH4).put(54) // 13, 14
    exec(programWithJump) shouldBe stack(pureWord(54))
  }

  "JUMPI opcode" should "do a conditional jump" in {
    def iprogram(i: Int) = prog
      .opcode(PUSH4) // 0
      .put(19) // 1
      .opcode(PUSH4) // 6
      .put(i) // 7
      .opcode(JUMPI) // 12
      .opcode(PUSH4).put(31) // 13, 14
      .opcode(PUSH4).put(54) // 19, 20

    def bprogram(b: Byte) = prog
      .opcode(PUSH4) // 0
      .put(16) // 1
      .opcode(PUSH4) // 6
      .put(b) // 7
      .opcode(JUMPI) // 9
      .opcode(PUSH4).put(31) // 10, 11
      .opcode(PUSH4).put(54) // 16, 17


    exec(iprogram(1)) shouldBe stack(pureWord(54))
    exec(iprogram(2)) shouldBe stack(pureWord(54))
    exec(iprogram(0)) shouldBe stack(pureWord(31), pureWord(54))
    exec(iprogram(-1)) shouldBe stack(pureWord(31), pureWord(54))


    exec(bprogram(1)) shouldBe stack(pureWord(54))
    exec(bprogram(2)) shouldBe stack(pureWord(54))
    exec(bprogram(0)) shouldBe stack(pureWord(31), pureWord(54))
    exec(bprogram(-1)) shouldBe stack(pureWord(31), pureWord(54))

  }

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
