package pravda.evm.translate.opcode

import com.google.protobuf.ByteString
import pravda.evm.debug.evm._
import utest.{TestSuite, Tests}
import utest._
import pravda.vm.sandbox.VmSandbox.Preconditions

object DebuggerDemo extends TestSuite {

  val tests = Tests {

    implicit val debugger = EvmDebugger
    implicit val showLog = EvmDebugger.debugLogShow(showStack = true, showHeap = false, showStorage = true)
    implicit val showLogs = EvmDebugger.showDebugLogContainer

    'Demo - {
      import pravda.evm.abi.parse.AbiParser
      import pravda.evm.debug.evm._
      import pravda.evm.parse.Parser
      import pravda.evm.{readSolidityABI, readSolidityBinFile}
      import pravda.vm.Data

      val preconditions = Preconditions(
        `watts-limit` = 100000L,
        stack = Seq(
          Data.Primitive.Bytes(
            ByteString
              .copyFrom((1 to 32).toArray.map(_.toByte))
              .concat(ByteString.copyFrom(Array.fill[Byte](32 - 32)(0)))),
          Data.Primitive.Int64(10),
          Data.Primitive.Utf8("emitTokens")
        )
//          Data.Primitive.BigInt(BigInt(69)),
//          Data.Primitive.Utf8("balanceOf")),
        //stack = Seq(Data.Primitive.Int64(10), Data.Primitive.Utf8("set")),
      )

      val Right(ops) = Parser.parseWithIndices(readSolidityBinFile("SimpleToken/SimpleToken.bin"))
      val Right(abi) = AbiParser.parseAbi(readSolidityABI("SimpleToken/SimpleToken.abi"))

      val Right(output) = EvmSandboxDebug.debugAddressedCode(preconditions, ops, abi)

      println(output)

    }

  }
}
