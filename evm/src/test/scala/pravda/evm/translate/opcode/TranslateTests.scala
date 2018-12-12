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

      asm ==> PravdaAssembler.parse(
        """
          |@__start_evm_program:
          |push bigint(128)
          |push bigint(64)
          |meta custom "MStoreev_2"
          |dup
          |push "get"
          |eq
          |push int32(120)
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_120
          |dup
          |push "set"
          |eq
          |push int32(78)
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_78
          |push "incorrect function name"
          |throw
          |@_lbl_73:
          |push bigint(0)
          |dup
          |stop
          |@_lbl_78:
          |push bigint(100000)
          |dup
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
          |push bigint(89)
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_89
          |push bigint(0)
          |dup
          |stop
          |@_lbl_89:
          |pop
          |push bigint(118)
          |push bigint(4)
          |dup
          |meta custom "CallDataSize"
          |swap
          |push bigint(-1)
          |mul
          |add
          |push int32(2)
          |dupn
          |add
          |swap
          |dup
          |dup
          |meta custom "CallDataLoad"
          |swap
          |push bigint(32)
          |add
          |swap
          |push int32(4)
          |swapn
          |push int32(3)
          |swapn
          |swap
          |pop
          |pop
          |pop
          |push bigint(160)
          |pop
          |jump @_lbl_160
          |@_lbl_118:
          |stop
          |@_lbl_120:
          |push bigint(100000)
          |dup
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
          |push bigint(131)
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_131
          |push bigint(0)
          |dup
          |stop
          |@_lbl_131:
          |pop
          |push bigint(138)
          |push bigint(170)
          |pop
          |jump @_lbl_170
          |@_lbl_138:
          |push bigint(64)
          |meta custom "Mload_3"
          |dup
          |push int32(3)
          |dupn
          |push int32(2)
          |dupn
          |meta custom "MStore_6"
          |push bigint(32)
          |add
          |push int32(3)
          |swapn
          |pop
          |pop
          |push bigint(64)
          |meta custom "Mload_3"
          |dup
          |push int32(3)
          |swapn
          |swap
          |push bigint(-1)
          |mul
          |add
          |swap
          |meta custom "Return"
          |@_lbl_160:
          |dup
          |push bigint(0)
          |push int32(2)
          |dupn
          |swap
          |sput
          |pop
          |pop
          |pop
          |jump @_lbl_118
          |@_lbl_170:
          |push bigint(0)
          |dup
          |sget
          |swap
          |pop
          |swap
          |pop
          |jump @_lbl_138
          |stop
        """.stripMargin)
    }
  }
}
