package pravda.vm
package opcode

import utest._
import VmUtils._
import Opcodes.int._
import serialization._


object HeapOpcodesTests extends TestSuite {

  val tests = Tests {
    'mputMget - {
      val program = prog
        .opcode(PUSHX).put(24)
        .opcode(MPUT)
      stackOfExec(program).length ==> 1
      stackOfExec(program.opcode(MGET)) ==> stack(int32ToData(24))
    }
  }

}
