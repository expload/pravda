package pravda.vm
package opcode

import VmUtils._
import pravda.vm.Opcodes.int._

import utest._
import serialization._


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

      assert(stackOfExec(program).isEmpty)
      stackOfExec(program1) ==> stack(int32ToData(325))
      stackOfExec(program2) ==> stack(int32ToData(325), int32ToData(325))
    }

    'stop - {
      val regularProgram = prog
        .opcode(PUSHX).put(31)
        .opcode(PUSHX).put(54)
      stackOfExec(regularProgram) ==> stack(int32ToData(31), int32ToData(54))

      val programWithDeadCode = prog
        .opcode(PUSHX).put(31)
        .opcode(STOP)
        .opcode(PUSHX).put(54) // DEAD code
      stackOfExec(programWithDeadCode) ==> stack(int32ToData(31))
    }

    'jump - {
      val regularProgram = prog
        .opcode(PUSHX).put(31)
        .opcode(PUSHX).put(54)
      stackOfExec(regularProgram) ==> stack(int32ToData(31), int32ToData(54))

      val programWithJump = prog
        .opcode(PUSHX) // 0
        .put(13) // 1
        .opcode(JUMP) // 6
        .opcode(PUSHX).put(31) // 7, 8
        .opcode(PUSHX).put(54) // 13, 14
      stackOfExec(programWithJump) ==> stack(int32ToData(54))
    }

    'jumpi - {
      def iprogram(i: Int) = prog
        .opcode(PUSHX) // 0
        .put(i) // 1
        .opcode(PUSHX) // 6
        .put(19) // 7
        .opcode(JUMPI) // 12
        .opcode(PUSHX).put(31) // 13, 14
        .opcode(PUSHX).put(54) // 19, 20

      def bprogram(b: Byte) = prog
        .opcode(PUSHX) // 0
        .put(b) // 1
        .opcode(PUSHX) // 6
        .put(15) // 7
        .opcode(JUMPI) // 8
        .opcode(PUSHX).put(31) // 9, 10
        .opcode(PUSHX).put(54) // 15, 16


      stackOfExec(iprogram(1)) ==> stack(int32ToData(54))
      stackOfExec(iprogram(2)) ==> stack(int32ToData(54))
      stackOfExec(iprogram(0)) ==> stack(int32ToData(31), int32ToData(54))
      stackOfExec(iprogram(-1)) ==> stack(int32ToData(54))


      stackOfExec(bprogram(1)) ==> stack(int32ToData(54))
      stackOfExec(bprogram(2)) ==> stack(int32ToData(54))
      stackOfExec(bprogram(0)) ==> stack(int32ToData(31), int32ToData(54))

    }

  }

}
