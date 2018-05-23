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
      exec(programm1).head ==> int32ToData(42)

      val programm2 = prog.opcode(PUSHX).put(-100)
      exec(programm2).head ==> int32ToData(-100)

      val programm3 = prog.opcode(PUSHX).put(0)
      exec(programm3).head ==> int32ToData(0)

      val programm4 = prog.opcode(PUSHX).put(100000)
      exec(programm4).head ==> int32ToData(100000)

      // 1 Byte
      val programm5 = prog.opcode(PUSHX).put(0xAB.toByte)
      exec(programm5).head ==> data(0xAB.toByte)

      val programm6 = prog.opcode(PUSHX).put(0x00.toByte)
      exec(programm6).head ==> data(0x00.toByte)

    }

    'pop - {

      val program1 = prog.withStack(int32ToData(34))
      assert(exec(program1).nonEmpty)

      val program2 = program1.opcode(POP)
      assert(exec(program2).isEmpty)

      val program3 = prog.withStack(int32ToData(34), int32ToData(76)).opcode(POP)
      exec(program3) ==> stack(int32ToData(34))
    }

    'dup - {

      val program1 = prog.withStack(int32ToData(13))
      exec(program1) ==> stack(int32ToData(13))

      val program2 = program1.opcode(DUP)
      exec(program2) ==> stack(int32ToData(13), int32ToData(13))

      val program3 = program2.opcode(DUP)
      exec(program3) ==> stack(int32ToData(13), int32ToData(13), int32ToData(13))

      val program4 = prog.withStack(int32ToData(13), int32ToData(15)).opcode(DUP)
      exec(program4) ==> stack(int32ToData(13), int32ToData(15), int32ToData(15))

    }

    'dupn - {

      val program = prog.opcode(PUSHX).put(55).opcode(PUSHX).put(34).opcode(PUSHX).put(2).opcode(PUSHX).put(3)
      exec(program.opcode(PUSHX).put(1).opcode(DUPN)) ==> stack(int32ToData(55), int32ToData(34), int32ToData(2), int32ToData(3), int32ToData(3))
      exec(program.opcode(PUSHX).put(2).opcode(DUPN)) ==> stack(int32ToData(55), int32ToData(34), int32ToData(2), int32ToData(3), int32ToData(2))
      exec(program.opcode(PUSHX).put(3).opcode(DUPN)) ==> stack(int32ToData(55), int32ToData(34), int32ToData(2), int32ToData(3), int32ToData(34))
      exec(program.opcode(PUSHX).put(4).opcode(DUPN)) ==> stack(int32ToData(55), int32ToData(34), int32ToData(2), int32ToData(3), int32ToData(55))

    }

    'swap - {

      val program1 = prog.withStack(int32ToData(55), int32ToData(55), int32ToData(2), int32ToData(3))
      exec(program1) ==> stack(int32ToData(55), int32ToData(55), int32ToData(2), int32ToData(3))

      exec(program1.opcode(SWAP)) ==> stack(int32ToData(55), int32ToData(55), int32ToData(3), int32ToData(2))

    }

    'swapn - {
 
      val program = prog.opcode(PUSHX).put(55).opcode(PUSHX).put(34).opcode(PUSHX).put(2).opcode(PUSHX).put(3)
      exec(program.opcode(PUSHX).put(1).opcode(SWAPN)) ==> stack(int32ToData(55), int32ToData(34), int32ToData(2), int32ToData(3))
      exec(program.opcode(PUSHX).put(2).opcode(SWAPN)) ==> stack(int32ToData(55), int32ToData(34), int32ToData(3), int32ToData(2))
      exec(program.opcode(PUSHX).put(3).opcode(SWAPN)) ==> stack(int32ToData(55), int32ToData(3), int32ToData(2), int32ToData(34))
      exec(program.opcode(PUSHX).put(4).opcode(SWAPN)) ==> stack(int32ToData(3), int32ToData(34), int32ToData(2), int32ToData(55))

    }


    'slice {
      val program = prog.opcode(PUSHX).put(hex"0d 11 2b 35").opcode(SLICE).put(1).put(3)
      exec(program) ==> stack(data(hex"11 2b"))
    }

    'concat - {
      val program = prog.opcode(PUSHX).put(hex"2b 35").opcode(PUSHX).put(hex"0d 11").opcode(CONCAT)
      exec(program) ==> stack(data(hex"0d 11 2b 35"))
    }
    
  }

}
