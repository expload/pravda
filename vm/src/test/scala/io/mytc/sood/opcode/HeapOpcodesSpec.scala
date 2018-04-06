package io.mytc.sood
package opcode

import serialize._
import vm.Opcodes.int._
import org.scalatest.{FlatSpec, Matchers}


class HeapOpcodesSpec extends FlatSpec with Matchers {

  "MPUT and MGET opcodes" should "store and load data from memory" in {
    val program = prog
      .opcode(PUSHX).put(24)
      .opcode(MPUT)
    exec(program) should have length 1
    exec(program) should not be stack(pureWord(24))
    exec(program.opcode(MGET)) shouldBe stack(pureWord(24))
  }

}
