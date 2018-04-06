package io.mytc.sood.vm

import VmUtils._
import Opcodes.int._
import org.scalatest.{FlatSpec, Matchers}

class ProcedureSpec extends FlatSpec with Matchers {

  def skip(p: Program): Program = prog
    .withStack(
      int32ToWord(p.length + 1)
    )
    .opcode(JUMP) +
    p

  def procedure(program: Program): Program = {
    program
      .opcode(SWAP)
      .opcode(JUMP)
  }

  implicit class ProgramWithProcedures(p: Program) {
    def call(procPosition: Int): Program = {
      p
        .opcode(PUSH4)
        .put(p.length + 11) // 11 - is the length of this program
        .opcode(PUSH4)
        .put(procPosition)
        .opcode(JUMP)
    }
  }

  "Procedure call" should "be able for implementation" in {
    // A simple procedure that gets nothing and returns contant number
    val proc = procedure {
      prog
        .opcode(PUSH4).put(123) // result
    }
    val program = skip(proc)

    exec(program) shouldBe empty
    exec(program.call(1)) shouldBe stack(int32ToWord(123))
    exec(program.call(1).call(1)) shouldBe stack(int32ToWord(123), int32ToWord(123))

  }
}
