package pravda.evm.translate.opcode

import fastparse.byte.all._
import pravda.evm.EVM._
import pravda.vm.Opcodes
import pravda.vm.asm.Operation
import utest._

object TranslateTests extends TestSuite {

  val tests = Tests {

    import SimpleTranslation._
    "PUSH" - {
      evmOpToOps(Push(hex"0x80")) ==> Right(List(pushBigInt(BigInt(128))))
    }

    "DUP" - {
      evmOpToOps(Dup(1)) ==> Right(List(Operation(Opcodes.DUP)))
      evmOpToOps(Dup(2)) ==> Right(List(pushInt(2), Operation(Opcodes.DUPN)))
    }

    "SWAP" - {
      evmOpToOps(Swap(1)) ==> Right(List(Operation(Opcodes.SWAP)))
      evmOpToOps(Swap(2)) ==> Right(List(pushInt(3), Operation(Opcodes.SWAPN)))
    }
  }

}
