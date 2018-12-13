package pravda.evm.translate.opcode

import fastparse.byte.all._
import pravda.evm.EVM._
import pravda.evm.abi.parse.AbiParser
import pravda.evm.parse.Parser
import pravda.evm.{readSolidityABI, readSolidityBinFile}
import pravda.evm.translate.Translator
import pravda.vm.Opcodes
import pravda.vm.asm.{Operation, PravdaAssembler}
import utest._

object TranslateTests extends TestSuite {

  val tests = Tests {

    import SimpleTranslation._

    'Basic - {
      "push" - {
        evmOpToOps(Push(hex"0x80")) ==> Right(List(pushBigInt(BigInt(128))))
      }

      "dup" - {
        evmOpToOps(Dup(1)) ==> Right(List(Operation(Opcodes.DUP)))
        evmOpToOps(Dup(2)) ==> Right(List(pushInt(2), Operation(Opcodes.DUPN)))
      }

      "swap" - {
        evmOpToOps(Swap(1)) ==> Right(List(Operation(Opcodes.SWAP)))
        evmOpToOps(Swap(2)) ==> Right(List(pushInt(3), Operation(Opcodes.SWAPN)))
      }
    }

    'Contracts - {
      val Right(ops) = Parser.parseWithIndices(readSolidityBinFile("SimpleStorage.bin"))
      val Right(abi) = AbiParser.parseAbi(readSolidityABI("SimpleStorageABIj.json"))
      val Right(asm) = Translator.translateActualContract(ops, abi)

      println(PravdaAssembler.render(asm))

      PravdaAssembler.render(asm) ==>
        """@__start_evm_program:
          |push int32(1024)
          |push int8(1)
          |new_array
          |push x8000000000000000000000000000000000000000000000000000000000000000
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |push int32(3)
          |dupn
          |push int8(7)
          |scall
          |push int32(2)
          |swapn
          |pop
          |swap
          |dup
          |push "get"
          |eq
          |jumpi @_lbl_120
          |dup
          |push "set"
          |eq
          |jumpi @_lbl_78
          |push "incorrect function name"
          |throw
          |@_lbl_73:
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_78:
          |push bigint(10)
          |push int8(14)
          |cast
          |dup
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push x5900000000000000000000000000000000000000000000000000000000000000
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_89
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_89:
          |pop
          |push x7600000000000000000000000000000000000000000000000000000000000000
          |push x0400000000000000000000000000000000000000000000000000000000000000
          |dup
          |push x04
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |swap
          |push bigint(-1)
          |mul
          |add
          |push int8(14)
          |cast
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |add
          |push int8(14)
          |cast
          |swap
          |dup
          |dup
          |pop
          |push x1234
          |swap
          |push x2000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |add
          |push int8(14)
          |cast
          |swap
          |push int32(4)
          |swapn
          |push int32(3)
          |swapn
          |swap
          |pop
          |pop
          |pop
          |push xA000000000000000000000000000000000000000000000000000000000000000
          |pop
          |jump @_lbl_160
          |@_lbl_118:
          |stop
          |@_lbl_120:
          |push bigint(10)
          |push int8(14)
          |cast
          |dup
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push x8300000000000000000000000000000000000000000000000000000000000000
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_131
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_131:
          |pop
          |push x8A00000000000000000000000000000000000000000000000000000000000000
          |push xAA00000000000000000000000000000000000000000000000000000000000000
          |pop
          |jump @_lbl_170
          |@_lbl_138:
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |push int32(4)
          |dupn
          |push int8(6)
          |scall
          |dup
          |push int32(3)
          |dupn
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(7)
          |dupn
          |push int8(7)
          |scall
          |push int32(6)
          |swapn
          |pop
          |push x2000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |add
          |push int8(14)
          |cast
          |push int32(3)
          |swapn
          |pop
          |pop
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |push int32(4)
          |dupn
          |push int8(6)
          |scall
          |dup
          |push int32(3)
          |swapn
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |swap
          |push bigint(-1)
          |mul
          |add
          |push int8(14)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(4)
          |dupn
          |push int32(8)
          |scall
          |swap
          |pop
          |swap
          |pop
          |stop
          |@_lbl_160:
          |dup
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int32(2)
          |dupn
          |swap
          |sput
          |pop
          |pop
          |pop
          |jump @_lbl_118
          |@_lbl_170:
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |sget
          |swap
          |pop
          |swap
          |pop
          |jump @_lbl_138
          |stop""" .stripMargin
    }
  }
}
