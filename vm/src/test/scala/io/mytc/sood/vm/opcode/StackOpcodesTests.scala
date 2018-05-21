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
      exec(programm1).head ==> data(42)

      val programm2 = prog.opcode(PUSHX).put(-100)
      exec(programm2).head ==> data(-100)

      val programm3 = prog.opcode(PUSHX).put(0)
      exec(programm3).head ==> data(0)

      val programm4 = prog.opcode(PUSHX).put(100000)
      exec(programm4).head ==> data(100000)

      // 1 Byte
      val programm5 = prog.opcode(PUSHX).put(0xAB.toByte)
      exec(programm5).head ==> binaryData(0xAB)

      val programm6 = prog.opcode(PUSHX).put(0x00.toByte)
      exec(programm6).head ==> binaryData(0x00)

    }

    'pop - {

      val program1 = prog.withStack(data(34))
      assert(exec(program1).nonEmpty)

      val program2 = program1.opcode(POP)
      assert(exec(program2).isEmpty)

      val program3 = prog.withStack(data(34), data(76)).opcode(POP)
      exec(program3) ==> stack(data(34))
    }

    'dup - {

      val program1 = prog.withStack(data(13))
      exec(program1) ==> stack(data(13))

      val program2 = program1.opcode(DUP)
      exec(program2) ==> stack(data(13), data(13))

      val program3 = program2.opcode(DUP)
      exec(program3) ==> stack(data(13), data(13), data(13))

      val program4 = prog.withStack(data(13), data(15)).opcode(DUP)
      exec(program4) ==> stack(data(13), data(15), data(15))

    }

    'dupn - {

      val program = prog.opcode(PUSHX).put(55).opcode(PUSHX).put(34).opcode(PUSHX).put(2).opcode(PUSHX).put(3)
      exec(program.opcode(PUSHX).put(1).opcode(DUPN)) ==> stack(data(55), data(34), data(2), data(3), data(3))
      exec(program.opcode(PUSHX).put(2).opcode(DUPN)) ==> stack(data(55), data(34), data(2), data(3), data(2))
      exec(program.opcode(PUSHX).put(3).opcode(DUPN)) ==> stack(data(55), data(34), data(2), data(3), data(34))
      exec(program.opcode(PUSHX).put(4).opcode(DUPN)) ==> stack(data(55), data(34), data(2), data(3), data(55))

    }

    'swap - {

      val program1 = prog.withStack(data(55), data(55), data(2), data(3))
      exec(program1) ==> stack(data(55), data(55), data(2), data(3))

      exec(program1.opcode(SWAP)) ==> stack(data(55), data(55), data(3), data(2))

    }

    'swapn - {
 
      val program = prog.opcode(PUSHX).put(55).opcode(PUSHX).put(34).opcode(PUSHX).put(2).opcode(PUSHX).put(3)
      exec(program.opcode(PUSHX).put(1).opcode(SWAPN)) ==> stack(data(55), data(34), data(2), data(3))
      exec(program.opcode(PUSHX).put(2).opcode(SWAPN)) ==> stack(data(55), data(34), data(3), data(2))
      exec(program.opcode(PUSHX).put(3).opcode(SWAPN)) ==> stack(data(55), data(3), data(2), data(34))
      exec(program.opcode(PUSHX).put(4).opcode(SWAPN)) ==> stack(data(3), data(34), data(2), data(55))

    }


    'slice {
      val program = prog.opcode(PUSHX).put(bytes(13, 17, 43, 53)).opcode(SLICE).put(1).put(3)
      exec(program) ==> stack(binaryData(17, 43))
    }

    'concat - {
      val program = prog.opcode(PUSHX).put(bytes(43, 53)).opcode(PUSHX).put(bytes(13, 17)).opcode(CONCAT)
      exec(program) ==> stack(binaryData(13, 17, 43, 53))
    }
    
  }

}
