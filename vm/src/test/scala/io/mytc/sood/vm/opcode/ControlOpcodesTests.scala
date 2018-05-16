package io.mytc.sood.vm
package opcode

import VmUtils._
import io.mytc.sood.vm.Opcodes.int._

import utest._

object ControlOpcodesTests extends TestSuite {

  val tests = Tests {

    'callRet - {
      val program = prog
        .opcode(PUSHX).put(14)
        .opcode(JUMP) // jump over the procedure
        .opcode(PUSHX).put(325).opcode(RET) // procedure
      val program1 = program
        .opcode(PUSHX).put(7).opcode(CALL) // call procedure
      val program2 = program
        .opcode(PUSHX).put(7).opcode(CALL) // call 1
        .opcode(PUSHX).put(7).opcode(CALL) // call 2

      assert(exec(program).isEmpty)
      exec(program1) ==> stack(data(325))
      exec(program2) ==> stack(data(325), data(325))
    }

    'stop - {
      val regularProgram = prog
        .opcode(PUSHX).put(31)
        .opcode(PUSHX).put(54)
      exec(regularProgram) ==> stack(data(31), data(54))

      val programWithDeadCode = prog
        .opcode(PUSHX).put(31)
        .opcode(STOP)
        .opcode(PUSHX).put(54) // DEAD code
      exec(programWithDeadCode) ==> stack(data(31))
    }

    'jump - {
      val regularProgram = prog
        .opcode(PUSHX).put(31)
        .opcode(PUSHX).put(54)
      exec(regularProgram) ==> stack(data(31), data(54))

      val programWithJump = prog
        .opcode(PUSHX) // 0
        .put(13) // 1
        .opcode(JUMP) // 6
        .opcode(PUSHX).put(31) // 7, 8
        .opcode(PUSHX).put(54) // 13, 14
      exec(programWithJump) ==> stack(data(54))
    }

    'jumpi - {
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


      exec(iprogram(1)) ==> stack(data(54))
      exec(iprogram(2)) ==> stack(data(54))
      exec(iprogram(0)) ==> stack(data(31), data(54))
      exec(iprogram(-1)) ==> stack(data(54))


      exec(bprogram(1)) ==> stack(data(54))
      exec(bprogram(2)) ==> stack(data(54))
      exec(bprogram(0)) ==> stack(data(31), data(54))

    }

  }

}
