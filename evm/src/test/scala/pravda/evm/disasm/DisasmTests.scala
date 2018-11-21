package pravda.evm.disasm

import pravda.evm.EVM._
import pravda.evm._
import pravda.evm.parse.Parser
import utest._

object DisasmTests extends TestSuite {

  val tests = Tests {
    'Disasm - {
      val bytes = evm.readSolidityBinFile("SimpleStorage.bin")

      Parser.parseWithIndices(bytes).map { ops =>
        Disasm(ops, bytes.length).map(newOps =>
          ops.zip(newOps).foreach {
            case ((ind, JumpI), (ind1, JumpI(ind2, _))) =>
              ind ==> ind2
            case ((ind, Jump), (ind1, Jump(ind2, _))) =>
              ind ==> ind2
            case (a, b) => a ==> b
        })
      }
    }
  }
}
