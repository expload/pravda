package io.mytc.sood
package opcode

import io.mytc.sood.vm.Opcodes.int._
import io.mytc.sood.serialize._
import org.scalatest.{FlatSpec, Matchers}

class StackOpcodesSpec extends FlatSpec with Matchers {

  "PUSHX command" should "push word to the stack" in {

    // 4 Bytes
    val programm1 = prog.opcode(PUSHX).put(42)
    exec(programm1).head shouldBe pureWord(42)

    val programm2 = prog.opcode(PUSHX).put(-100)
    exec(programm2).head shouldBe pureWord(-100)

    val programm3 = prog.opcode(PUSHX).put(0)
    exec(programm3).head shouldBe pureWord(0)

    val programm4 = prog.opcode(PUSHX).put(100000)
    exec(programm4).head shouldBe pureWord(100000)

    // 1 Byte
    val programm5 = prog.opcode(PUSHX).put(0xAB.toByte)
    exec(programm5).head shouldBe pureWord(0xAB.toByte)

    val programm6 = prog.opcode(PUSHX).put(0x00.toByte)
    exec(programm6).head shouldBe pureWord(0x00.toByte)

  }

  "POP command" should "pop word from the stack" in {

    val program1 = prog.withStack(pureWord(34))
    exec(program1) should not be empty

    val program2 = program1.opcode(POP)
    exec(program2) shouldBe empty

    val program3 = prog.withStack(pureWord(34), pureWord(76)).opcode(POP)
    exec(program3) shouldBe stack(pureWord(34))
  }

  "DUP command" should "duplicate top value of the stack" in {

    val program1 = prog.withStack(pureWord(13))
    exec(program1) shouldBe stack(pureWord(13))

    val program2 = program1.opcode(DUP)
    exec(program2) shouldBe stack(pureWord(13), pureWord(13))

    val program3 = program2.opcode(DUP)
    exec(program3) shouldBe stack(pureWord(13), pureWord(13), pureWord(13))

    val program4 = prog.withStack(pureWord(13), pureWord(15)).opcode(DUP)
    exec(program4) shouldBe stack(pureWord(13), pureWord(15), pureWord(15))

  }

  "SWAP command" should "swap two top values in the stack" in {

    val program1 = prog.withStack(pureWord(55), pureWord(55),pureWord(2), pureWord(3))
    exec(program1) shouldBe stack(pureWord(55), pureWord(55), pureWord(2), pureWord(3))

    exec(program1.opcode(SWAP)) shouldBe stack(pureWord(55), pureWord(55), pureWord(3), pureWord(2))

  }
}
