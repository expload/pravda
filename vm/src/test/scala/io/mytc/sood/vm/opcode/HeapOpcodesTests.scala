package io.mytc.sood.vm
package opcode

import utest._
import VmUtils._
import Opcodes.int._

object HeapOpcodesTests extends TestSuite {

  val tests = Tests {
    'mputMget - {
      val program = prog
        .opcode(PUSHX).put(24)
        .opcode(MPUT)
      exec(program).length ==> 1
      exec(program.opcode(MGET)) ==> stack(data(24))
    }
  }

}
