package pravda.evm
package translate.opcode

import pravda.evm.abi.parse.AbiParser
import pravda.evm.parse.Parser
import pravda.evm.utils._
import pravda.vm.{Data, VmSandbox}
import utest._

object FullContractTests extends TestSuite {
  import VmSandbox.{ExpectationsWithoutWatts => Expectations}

  val tests = Tests {
    'SimpleStorage - {
      val preconditions = VmSandbox.Preconditions(`watts-limit` = 10000L,
                                                  stack = Seq(Data.Primitive.Utf8("get")),
                                                  storage = Map(evmWord(Array(0)) -> evmWord(Array(1))))

      val Right(ops) = Parser.parseWithIndices(readSolidityBinFile("SimpleStorage.bin"))
      val Right(abi) = AbiParser.parseAbi(readSolidityABI("SimpleStorageABIj.json"))

      EvmSandbox.runAddressedCode(preconditions, ops, abi) ==> Right(Expectations())
    }
  }
}
