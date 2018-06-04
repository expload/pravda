package pravda.vm
package opcode

import VmUtils._
import Opcodes.int._
import utest._

import serialization._

import pravda.common.bytes.hex._

object StackOpcodesTests extends TestSuite {

  val tests = Tests {
    'push - {

      // 4 Bytes
      val programm1 = prog.opcode(PUSHX).put(42)
      stackOfExec(programm1).head ==> int32ToData(42)

      val programm2 = prog.opcode(PUSHX).put(-100)
      stackOfExec(programm2).head ==> int32ToData(-100)

      val programm3 = prog.opcode(PUSHX).put(0)
      stackOfExec(programm3).head ==> int32ToData(0)

      val programm4 = prog.opcode(PUSHX).put(100000)
      stackOfExec(programm4).head ==> int32ToData(100000)

      // 1 Byte
      val programm5 = prog.opcode(PUSHX).put(0xAB.toByte)
      stackOfExec(programm5).head ==> data(0xAB.toByte)

      val programm6 = prog.opcode(PUSHX).put(0x00.toByte)
      stackOfExec(programm6).head ==> data(0x00.toByte)

    }

    'pop - {

      val program1 = prog.withStack(int32ToData(34))
      assert(stackOfExec(program1).nonEmpty)

      val program2 = program1.opcode(POP)
      assert(stackOfExec(program2).isEmpty)

      val program3 = prog.withStack(int32ToData(34), int32ToData(76)).opcode(POP)
      stackOfExec(program3) ==> stack(int32ToData(34))
    }

    'dup - {

      val program1 = prog.withStack(int32ToData(13))
      stackOfExec(program1) ==> stack(int32ToData(13))

      val program2 = program1.opcode(DUP)
      stackOfExec(program2) ==> stack(int32ToData(13), int32ToData(13))

      val program3 = program2.opcode(DUP)
      stackOfExec(program3) ==> stack(int32ToData(13), int32ToData(13), int32ToData(13))

      val program4 = prog.withStack(int32ToData(13), int32ToData(15)).opcode(DUP)
      stackOfExec(program4) ==> stack(int32ToData(13), int32ToData(15), int32ToData(15))

    }

    'dupn - {

      val program = prog.opcode(PUSHX).put(55).opcode(PUSHX).put(34).opcode(PUSHX).put(2).opcode(PUSHX).put(3)
      stackOfExec(program.opcode(PUSHX).put(1).opcode(DUPN)) ==> stack(int32ToData(55), int32ToData(34), int32ToData(2), int32ToData(3), int32ToData(3))
      stackOfExec(program.opcode(PUSHX).put(2).opcode(DUPN)) ==> stack(int32ToData(55), int32ToData(34), int32ToData(2), int32ToData(3), int32ToData(2))
      stackOfExec(program.opcode(PUSHX).put(3).opcode(DUPN)) ==> stack(int32ToData(55), int32ToData(34), int32ToData(2), int32ToData(3), int32ToData(34))
      stackOfExec(program.opcode(PUSHX).put(4).opcode(DUPN)) ==> stack(int32ToData(55), int32ToData(34), int32ToData(2), int32ToData(3), int32ToData(55))

    }

    'swap - {

      val program1 = prog.withStack(int32ToData(55), int32ToData(55), int32ToData(2), int32ToData(3))
      stackOfExec(program1) ==> stack(int32ToData(55), int32ToData(55), int32ToData(2), int32ToData(3))

      stackOfExec(program1.opcode(SWAP)) ==> stack(int32ToData(55), int32ToData(55), int32ToData(3), int32ToData(2))

    }

    'swapn - {
 
      val program = prog.opcode(PUSHX).put(55).opcode(PUSHX).put(34).opcode(PUSHX).put(2).opcode(PUSHX).put(3)
      stackOfExec(program.opcode(PUSHX).put(1).opcode(SWAPN)) ==> stack(int32ToData(55), int32ToData(34), int32ToData(2), int32ToData(3))
      stackOfExec(program.opcode(PUSHX).put(2).opcode(SWAPN)) ==> stack(int32ToData(55), int32ToData(34), int32ToData(3), int32ToData(2))
      stackOfExec(program.opcode(PUSHX).put(3).opcode(SWAPN)) ==> stack(int32ToData(55), int32ToData(3), int32ToData(2), int32ToData(34))
      stackOfExec(program.opcode(PUSHX).put(4).opcode(SWAPN)) ==> stack(int32ToData(3), int32ToData(34), int32ToData(2), int32ToData(55))

    }


    'slice {
      val program = prog.opcode(PUSHX).put(hex"0d 11 2b 35").opcode(SLICE).put(1).put(3)
      stackOfExec(program) ==> stack(data(hex"11 2b"))
    }

    'concat - {
      val program = prog.opcode(PUSHX).put(hex"2b 35").opcode(PUSHX).put(hex"0d 11").opcode(CONCAT)
      stackOfExec(program) ==> stack(data(hex"0d 11 2b 35"))
    }
    
  }

}
