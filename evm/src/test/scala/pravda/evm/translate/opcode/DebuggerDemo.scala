package pravda.evm.translate.opcode



import pravda.evm.debug.evm._
import utest.{TestSuite, Tests}
import utest._
import pravda.vm.sandbox.VmSandbox.Preconditions
import pravda.evm.utils._
object DebuggerDemo extends TestSuite {


  val tests = Tests {

    import cats.instances.list._

    implicit val debugger = EvmDebugger
    implicit val showLog = EvmDebugger.DebugLogShow(showStack = true,showHeap = false,showStorage = true)
    implicit val showLogs = EvmDebugger.showDebugLogContainer

    'Demo - {
      import pravda.evm.abi.parse.AbiParser
      import pravda.evm.debug.evm._
      import pravda.evm.parse.Parser
      import pravda.evm.{readSolidityABI, readSolidityBinFile}
      import pravda.vm.Data


      val preconditions = Preconditions(`watts-limit` = 100000L,
        stack = Seq(Data.Primitive.Utf8("get")),
        storage = Map(evmWord(Array(0)) -> evmWord(Array(1)))
        //stack = Seq(Data.Primitive.Int64(10), Data.Primitive.Utf8("set")),
      )

      val Right(ops) = Parser.parseWithIndices(readSolidityBinFile("SimpleStorage/SimpleStorage.bin"))
      val Right(abi) = AbiParser.parseAbi(readSolidityABI("SimpleStorage/SimpleStorage.abi"))

      val Right(output) = EvmSandboxDebug.debugAddressedCode(preconditions, ops, abi)

      println(output)
      true ==> true

    }

  }
}

