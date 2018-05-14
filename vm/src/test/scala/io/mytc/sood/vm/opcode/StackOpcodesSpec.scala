package io.mytc.sood.vm
package opcode

import VmUtils._
import Opcodes.int._
import org.scalatest.{FlatSpec, Matchers}

class StackOpcodesSpec extends FlatSpec with Matchers {

  "PUSH command" should "push word to the stack" in {

    // 4 Bytes
    val programm1 = prog.opcode(PUSHX).put(42)
    exec(programm1).head shouldBe data(42)

    val programm2 = prog.opcode(PUSHX).put(-100)
    exec(programm2).head shouldBe data(-100)

    val programm3 = prog.opcode(PUSHX).put(0)
    exec(programm3).head shouldBe data(0)

    val programm4 = prog.opcode(PUSHX).put(100000)
    exec(programm4).head shouldBe data(100000)

    // 1 Byte
    val programm5 = prog.opcode(PUSHX).put(0xAB.toByte)
    exec(programm5).head shouldBe binaryData(0xAB)

    val programm6 = prog.opcode(PUSHX).put(0x00.toByte)
    exec(programm6).head shouldBe binaryData(0x00)

  }

  "POP command" should "pop word from the stack" in {

    val program1 = prog.withStack(data(34))
    exec(program1) should not be empty

    val program2 = program1.opcode(POP)
    exec(program2) shouldBe empty

    val program3 = prog.withStack(data(34), data(76)).opcode(POP)
    exec(program3) shouldBe stack(data(34))
  }

  "DUP command" should "duplicate top value of the stack" in {

    val program1 = prog.withStack(data(13))
    exec(program1) shouldBe stack(data(13))

    val program2 = program1.opcode(DUP)
    exec(program2) shouldBe stack(data(13), data(13))

    val program3 = program2.opcode(DUP)
    exec(program3) shouldBe stack(data(13), data(13), data(13))

    val program4 = prog.withStack(data(13), data(15)).opcode(DUP)
    exec(program4) shouldBe stack(data(13), data(15), data(15))

  }

  "DUPN command" should "duplicate n-th value of the stack" in {

    val program = prog.opcode(PUSHX).put(55).opcode(PUSHX).put(34).opcode(PUSHX).put(2).opcode(PUSHX).put(3)
    exec(program.opcode(PUSHX).put(1).opcode(DUPN)) shouldBe stack(data(55), data(34), data(2), data(3), data(3))
    exec(program.opcode(PUSHX).put(2).opcode(DUPN)) shouldBe stack(data(55), data(34), data(2), data(3), data(2))
    exec(program.opcode(PUSHX).put(3).opcode(DUPN)) shouldBe stack(data(55), data(34), data(2), data(3), data(34))
    exec(program.opcode(PUSHX).put(4).opcode(DUPN)) shouldBe stack(data(55), data(34), data(2), data(3), data(55))

  }

  "SWAP command" should "swap two top values in the stack" in {

    val program1 = prog.withStack(data(55), data(55),data(2), data(3))
    exec(program1) shouldBe stack(data(55), data(55), data(2), data(3))

    exec(program1.opcode(SWAP)) shouldBe stack(data(55), data(55), data(3), data(2))

  }

  "SWAPN command" should "swap the top value and the n-th value in the stack" in {

    val program = prog.opcode(PUSHX).put(55).opcode(PUSHX).put(34).opcode(PUSHX).put(2).opcode(PUSHX).put(3)
    exec(program.opcode(PUSHX).put(1).opcode(SWAPN)) shouldBe stack(data(55), data(34), data(2), data(3))
    exec(program.opcode(PUSHX).put(2).opcode(SWAPN)) shouldBe stack(data(55), data(34), data(3), data(2))
    exec(program.opcode(PUSHX).put(3).opcode(SWAPN)) shouldBe stack(data(55), data(3), data(2), data(34))
    exec(program.opcode(PUSHX).put(4).opcode(SWAPN)) shouldBe stack(data(3), data(34), data(2), data(55))

  }


  "SLICE command" should  "slice top word" in {
    val program = prog.opcode(PUSHX).put(bytes(13, 17, 43, 53)).opcode(SLICE).put(1).put(3)
    exec(program) shouldBe stack(binaryData(17, 43))
  }

  "CONCAT command" should  "concatenate top word" in {
    val program = prog.opcode(PUSHX).put(bytes(43, 53)).opcode(PUSHX).put(bytes(13, 17)).opcode(CONCAT)
    exec(program) shouldBe stack(binaryData(13, 17, 43, 53))
  }

}
