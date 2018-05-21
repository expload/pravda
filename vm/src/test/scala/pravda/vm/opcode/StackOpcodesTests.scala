package pravda.vm
package opcode

import VmUtils._
import Opcodes.int._
import utest._

object StackOpcodesTests extends TestSuite {

  val tests = Tests {
    'push - {

      // 4 Bytes
      val programm1 = prog.opcode(PUSHX).put(42)
      exec(programm1).head ==> int2Data(42)

      val programm2 = prog.opcode(PUSHX).put(-100)
      exec(programm2).head ==> int2Data(-100)

      val programm3 = prog.opcode(PUSHX).put(0)
      exec(programm3).head ==> int2Data(0)

      val programm4 = prog.opcode(PUSHX).put(100000)
      exec(programm4).head ==> int2Data(100000)

      // 1 Byte
      val programm5 = prog.opcode(PUSHX).put(0xAB.toByte)
      exec(programm5).head ==> data(0xAB.toByte)

      val programm6 = prog.opcode(PUSHX).put(0x00.toByte)
      exec(programm6).head ==> data(0x00.toByte)

    }

    'pop - {

      val program1 = prog.withStack(int2Data(34))
      assert(exec(program1).nonEmpty)

      val program2 = program1.opcode(POP)
      assert(exec(program2).isEmpty)

      val program3 = prog.withStack(int2Data(34), int2Data(76)).opcode(POP)
      exec(program3) ==> stack(int2Data(34))
    }

    'dup - {

      val program1 = prog.withStack(int2Data(13))
      exec(program1) ==> stack(int2Data(13))

      val program2 = program1.opcode(DUP)
      exec(program2) ==> stack(int2Data(13), int2Data(13))

      val program3 = program2.opcode(DUP)
      exec(program3) ==> stack(int2Data(13), int2Data(13), int2Data(13))

      val program4 = prog.withStack(int2Data(13), int2Data(15)).opcode(DUP)
      exec(program4) ==> stack(int2Data(13), int2Data(15), int2Data(15))

    }

    'dupn - {

      val program = prog.opcode(PUSHX).put(55).opcode(PUSHX).put(34).opcode(PUSHX).put(2).opcode(PUSHX).put(3)
      exec(program.opcode(PUSHX).put(1).opcode(DUPN)) ==> stack(int2Data(55), int2Data(34), int2Data(2), int2Data(3), int2Data(3))
      exec(program.opcode(PUSHX).put(2).opcode(DUPN)) ==> stack(int2Data(55), int2Data(34), int2Data(2), int2Data(3), int2Data(2))
      exec(program.opcode(PUSHX).put(3).opcode(DUPN)) ==> stack(int2Data(55), int2Data(34), int2Data(2), int2Data(3), int2Data(34))
      exec(program.opcode(PUSHX).put(4).opcode(DUPN)) ==> stack(int2Data(55), int2Data(34), int2Data(2), int2Data(3), int2Data(55))

    }

    'swap - {

      val program1 = prog.withStack(int2Data(55), int2Data(55), int2Data(2), int2Data(3))
      exec(program1) ==> stack(int2Data(55), int2Data(55), int2Data(2), int2Data(3))

      exec(program1.opcode(SWAP)) ==> stack(int2Data(55), int2Data(55), int2Data(3), int2Data(2))

    }

    'swapn - {
 
      val program = prog.opcode(PUSHX).put(55).opcode(PUSHX).put(34).opcode(PUSHX).put(2).opcode(PUSHX).put(3)
      exec(program.opcode(PUSHX).put(1).opcode(SWAPN)) ==> stack(int2Data(55), int2Data(34), int2Data(2), int2Data(3))
      exec(program.opcode(PUSHX).put(2).opcode(SWAPN)) ==> stack(int2Data(55), int2Data(34), int2Data(3), int2Data(2))
      exec(program.opcode(PUSHX).put(3).opcode(SWAPN)) ==> stack(int2Data(55), int2Data(3), int2Data(2), int2Data(34))
      exec(program.opcode(PUSHX).put(4).opcode(SWAPN)) ==> stack(int2Data(3), int2Data(34), int2Data(2), int2Data(55))

    }


    'slice {
      val program = prog.opcode(PUSHX).put(bytes(13, 17, 43, 53)).opcode(SLICE).put(1).put(3)
      exec(program) ==> stack(data(17.toByte, 43.toByte))
    }

    'concat - {
      val program = prog.opcode(PUSHX).put(bytes(43, 53)).opcode(PUSHX).put(bytes(13, 17)).opcode(CONCAT)
      exec(program) ==> stack(data(13.toByte, 17.toByte, 43.toByte, 53.toByte))
    }
    
  }

}
