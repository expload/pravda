package io.mytc.sood.vm

import VmUtils._
import Opcodes.int._
import org.scalatest.{FlatSpec, Matchers}

class ProcedureSpec extends FlatSpec with Matchers {

  def skip(p: Program): Program = prog
    .withStack(
      data(p.length + 1)
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
        .opcode(PUSHX)
        .put(p.length + 13) // 13 - is the length of this program
        .opcode(PUSHX)
        .put(procPosition)
        .opcode(JUMP)
    }
  }

  "Procedure call" should "be able for implementation" in {
    // A simple procedure that gets nothing and returns contant number
    val proc = procedure {
      prog
        .opcode(PUSHX).put(123) // result
    }
    val program = skip(proc)

    exec(program) shouldBe empty
    exec(program.call(1)) shouldBe stack(data(123))
    exec(program.call(1).call(1)) shouldBe stack(data(123), data(123))

  }
}
