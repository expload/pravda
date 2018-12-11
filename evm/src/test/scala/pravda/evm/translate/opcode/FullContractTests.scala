package pravda.evm.translate.opcode

import pravda.evm.abi.parse.AbiParser
import pravda.evm.parse.Parser
import pravda.evm.{EvmSandbox, readSolidityABI, readSolidityBinFile}
import pravda.vm.VmSandbox
import utest._

object FullContractTests extends TestSuite {
  import VmSandbox.{ExpectationsWithoutWatts => Expectations}

  val tests = Tests {
    'SimpleStorage - {
      val preconditions = VmSandbox.Preconditions(`watts-limit` = 10000L)

      val Right(ops) = Parser.parseWithIndices(readSolidityBinFile("SimpleStorage.bin"))
      val Right(abi) = AbiParser.parseAbi(readSolidityABI("SimpleStorageABIj.json"))

      EvmSandbox.runAddressedCode(preconditions, ops, abi) ==> Right(Expectations())
    }
  }
}
