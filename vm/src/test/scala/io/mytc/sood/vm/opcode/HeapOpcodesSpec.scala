package io.mytc.sood.vm
package opcode

import org.scalatest.{FlatSpec, Matchers}
import VmUtils._
import Opcodes.int._

class HeapOpcodesSpec extends FlatSpec with Matchers {

  "MPUT and MGET opcodes" should "store and load data from memory" in {
    val program = prog
      .opcode(PUSHX).put(24)
      .opcode(MPUT)
    exec(program) should have length 1
    exec(program) should not be stack(data(24))
    exec(program.opcode(MGET)) shouldBe stack(data(24))
  }

}
