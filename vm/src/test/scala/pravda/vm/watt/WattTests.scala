package pravda.vm
package watt

import utest._
import VmUtils._
import Opcodes._
import WattCounter._
import serialization._
import pravda.common.domain.NativeCoin
import pravda.common.bytes.hex._

object WattTests extends TestSuite {

  val tests = Tests {

    'simple - {
      val MemoryUsage = 100L

      exec(prog.opcode(STOP)).wattCounter.total ==> CpuBasic
      exec(prog.opcode(PUSHX).put(1)).wattCounter.total ==> MemoryUsage + CpuBasic

      val baseProg = prog.opcode(PUSHX).put(1).opcode(PUSHX).put(2)
      val baseCnt = exec(baseProg).wattCounter.total

      exec(baseProg.opcode(ADD)).wattCounter.total ==> baseCnt + CpuBasic + CpuSimpleArithmetic
      exec(baseProg.opcode(MUL)).wattCounter.total ==> baseCnt + CpuBasic + CpuArithmetic

      exec(prog.opcode(PUSHX).put(hex"ff00aa").opcode(PUSHX).put(coinsToData(NativeCoin.zero)).opcode(TRANSFER)).wattCounter.total ==>
        MemoryUsage + CpuBasic + CpuBasic + CpuStorageUse + CpuBasic

    }

  }

}
