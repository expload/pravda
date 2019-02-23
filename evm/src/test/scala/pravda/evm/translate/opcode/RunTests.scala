package pravda.evm.translate.opcode

import fastparse.byte.all._
import pravda.evm.EVM._
import pravda.evm.EvmSandbox
import pravda.evm.abi.parse.AbiParser.AbiFunction
import pravda.evm.utils._
import pravda.vm.sandbox.VmSandbox.{ExpectationsWithoutWatts, Preconditions}
import utest._

object RunTests extends TestSuite {

  val tests = Tests {

    val preconditions = Preconditions(`watts-limit` = 10000L)
    val abi = List(AbiFunction(true, "", Nil, Nil, true, "", None))

    "DUP" - {
      EvmSandbox.runCode(preconditions, List(Push(hex"0x01"), Dup(1)), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0x01)), evmWord(Array(0x01)))))

      EvmSandbox.runCode(preconditions, List(Push(hex"0x01"), Push(hex"0x02"), Dup(2)), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0x01)), evmWord(Array(0x02)), evmWord(Array(0x01)))))
    }

    "SWAP" - {
      EvmSandbox.runCode(preconditions, List(Push(hex"0x01"), Push(hex"0x02"), Swap(1)), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0x02)), evmWord(Array(0x01)))))

      EvmSandbox.runCode(preconditions, List(Push(hex"0x01"), Push(hex"0x02"), Push(hex"0x03"), Swap(2)), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0x03)), evmWord(Array(0x02)), evmWord(Array(0x01)))))
    }

    //TODO tests with overflow

    "ADD" - {
      EvmSandbox.runCode(preconditions, List(Push(hex"0x01"), Push(hex"0x02"), Add), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0x03)))))
    }

    "MUL" - {
      EvmSandbox.runCode(preconditions, List(Push(hex"0x02"), Push(hex"0x03"), Mul), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0x06)))))
    }

    "DIV" - {
      EvmSandbox.runCode(preconditions, List(Push(hex"0x02"), Push(hex"0x06"), Div), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0x03)))))
    }

    "MOD" - {
      EvmSandbox.runCode(preconditions, List(Push(hex"0x02"), Push(hex"0x05"), Mod), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0x01)))))
    }

    "SUB" - {
      EvmSandbox.runCode(preconditions, List(Push(hex"0x02"), Push(hex"0x05"), Sub), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0x03)))))
    }

    "OR" - {
      EvmSandbox.runCode(preconditions, List(Push(hex"0x05"), Push(hex"0x03"), Or), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0x07)))))
    }

    "AND" - {
      EvmSandbox.runCode(preconditions, List(Push(hex"0x05"), Push(hex"0x03"), And), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0x01)))))
    }

    "XOR" - {
      EvmSandbox.runCode(preconditions, List(Push(hex"0x05"), Push(hex"0x03"), Xor), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0x06)))))
    }

//    "BYTE" - {
//      val x = hex"0x426"
//      var pos = hex"0x1f"
//
//      EvmSandbox.runCode(preconditions, List(Push(x), Push(pos), Byte), abi) ==>
//        Right(ExpectationsWithoutWatts(stack = Seq(BigInt(scala.BigInt(x.last.toInt)))))
//
//      pos = hex"0x1e"
//      val expectation = x.reverse.tail.head.toInt
//      EvmSandbox.runCode(preconditions, List(Push(x), Push(pos), Byte), abi) ==>
//        Right(ExpectationsWithoutWatts(stack = Seq(BigInt(scala.BigInt(expectation)))))
//    }

    "LT" - {
      val x = hex"0x4"
      val y = hex"0x2"
      EvmSandbox.runCode(preconditions, List(Push(x), Push(y), Lt), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(1)))))

      EvmSandbox.runCode(preconditions, List(Push(y), Push(x), Lt), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0)))))
    }

    "GT" - {
      val x = hex"0x4"
      val y = hex"0x2"
      EvmSandbox.runCode(preconditions, List(Push(x), Push(y), Gt), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0)))))

      EvmSandbox.runCode(preconditions, List(Push(y), Push(x), Gt), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(1)))))
    }

    "EQ" - {
      val x = hex"0x4"
      val y = hex"0x2"
      EvmSandbox.runCode(preconditions, List(Push(x), Push(y), Eq), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(0)))))

      EvmSandbox.runCode(preconditions, List(Push(x), Push(x), Eq), abi) ==>
        Right(ExpectationsWithoutWatts(stack = Seq(evmWord(Array(1)))))
    }
  }
}
