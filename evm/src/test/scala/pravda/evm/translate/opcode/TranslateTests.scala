package pravda.evm.translate.opcode

import fastparse.byte.all._
import pravda.evm.EVM._
import pravda.evm.abi.parse.AbiParser
import pravda.evm.parse.Parser
import pravda.evm.{readSolidityABI, readSolidityBinFile}
import pravda.evm.translate.Translator
import pravda.vm.Opcodes
import pravda.vm.asm.{Operation, PravdaAssembler}
import pravda.evm.utils._
import utest._

object TranslateTests extends TestSuite {

  val tests = Tests {

    import SimpleTranslation._

    'Basic - {
      "push" - {
        evmOpToOps(Push(hex"0x80")) ==> Right(List(Operation.Push(evmWord(Array(-128)))))
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

      'SimpleStorage - {
        val Right(ops) = Parser.parseWithIndices(readSolidityBinFile("SimpleStorage.bin"))
        val Right(abi) = AbiParser.parseAbi(readSolidityABI("SimpleStorageABIj.json"))
        val Right(asm) = Translator.translateActualContract(ops, abi)

        PravdaAssembler.render(asm) ==>
          """@__start_evm_program:
          |push int32(1024)
          |push int8(1)
          |new_array
          |swap
          |dup
          |push "set"
          |eq
          |not
          |jumpi @not_set
          |push int32(3)
          |swapn
          |swap
          |push int32(2)
          |swapn
          |push x
          |swap
          |push int8(14)
          |cast
          |push int32(9)
          |scall
          |concat
          |push x00000000
          |concat
          |swap
          |push null
          |jump @_lbl_78
          |@not_set:
          |dup
          |push "get"
          |eq
          |not
          |jumpi @not_get
          |swap
          |push x
          |push x00000000
          |concat
          |swap
          |push null
          |jump @_lbl_120
          |@not_get:
          |@_lbl_73:
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_78:
          |@_lbl_89:
          |pop
          |push x7600000000000000000000000000000000000000000000000000000000000000
          |push x0400000000000000000000000000000000000000000000000000000000000000
          |dup
          |push int32(5)
          |dupn
          |length
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
          |push int8(9)
          |scall
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
          |push int8(9)
          |scall
          |swap
          |dup
          |dup
          |push int8(4)
          |cast
          |push int32(7)
          |dupn
          |swap
          |dup
          |push int32(32)
          |add
          |swap
          |slice
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
          |push int8(9)
          |scall
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
          |pop
          |pop
          |pop
          |stop
          |@_lbl_120:
          |@_lbl_131:
          |pop
          |push x8A00000000000000000000000000000000000000000000000000000000000000
          |push xAA00000000000000000000000000000000000000000000000000000000000000
          |pop
          |jump @_lbl_170
          |@_lbl_138:
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push bigint(32)
          |swap
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
          |push int32(6)
          |dupn
          |push int8(7)
          |scall
          |push int32(5)
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
          |push int8(9)
          |scall
          |push int32(3)
          |swapn
          |pop
          |pop
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push bigint(32)
          |swap
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
          |push int8(9)
          |scall
          |swap
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(3)
          |dupn
          |push int32(8)
          |scall
          |swap
          |pop
          |swap
          |pop
          |swap
          |jump @convert_result
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
          |call @stdlib_evm_sget
          |swap
          |pop
          |swap
          |pop
          |jump @_lbl_138
          |pop
          |pop
          |pop
          |stop
          |@stdlib_evm_sget:
          |dup
          |sexist
          |jumpi @stdlib_evm_sget_non_zero
          |pop
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |ret
          |@stdlib_evm_sget_non_zero:
          |sget
          |ret
          |@convert_result:
          |dup
          |push "get"
          |eq
          |not
          |jumpi @convert_result_not_get
          |pop
          |push int8(4)
          |cast
          |stop
          |@convert_result_not_get:
          |dup
          |push "set"
          |eq
          |not
          |jumpi @convert_result_not_set
          |pop
          |stop
          |@convert_result_not_set:""".stripMargin
      }

      'SimpleToken - {
        val Right(ops) = Parser.parseWithIndices(readSolidityBinFile("SimpleToken/SimpleToken.bin"))
        val Right(abi) = AbiParser.parseAbi(readSolidityABI("SimpleToken/SimpleToken.abi"))
        val Right(asm) = Translator.translateActualContract(ops, abi)

        PravdaAssembler.render(asm) ==>
        """@__start_evm_program:
          |push int32(1024)
          |push int8(1)
          |new_array
          |swap
          |dup
          |push "balances"
          |eq
          |not
          |jumpi @not_balances
          |push int32(3)
          |swapn
          |swap
          |push int32(2)
          |swapn
          |push x
          |swap
          |push int8(14)
          |cast
          |push int32(9)
          |scall
          |concat
          |push x00000000
          |concat
          |swap
          |push null
          |jump @_lbl_103
          |@not_balances:
          |dup
          |push "balanceOf"
          |eq
          |not
          |jumpi @not_balanceOf
          |push int32(3)
          |swapn
          |swap
          |push int32(2)
          |swapn
          |push x
          |swap
          |push int8(14)
          |cast
          |push int32(9)
          |scall
          |concat
          |push x00000000
          |concat
          |swap
          |push null
          |jump @_lbl_204
          |@not_balanceOf:
          |dup
          |push "transfer"
          |eq
          |not
          |jumpi @not_transfer
          |push int32(4)
          |swapn
          |push int32(3)
          |swapn
          |swap
          |push int32(3)
          |swapn
          |push int32(2)
          |swapn
          |push x
          |swap
          |push int8(14)
          |cast
          |push int32(9)
          |scall
          |concat
          |swap
          |push int8(14)
          |cast
          |push int32(9)
          |scall
          |concat
          |push x00000000
          |concat
          |swap
          |push null
          |jump @_lbl_305
          |@not_transfer:
          |dup
          |push "emitTokens"
          |eq
          |not
          |jumpi @not_emitTokens
          |push int32(4)
          |swapn
          |push int32(3)
          |swapn
          |swap
          |push int32(3)
          |swapn
          |push int32(2)
          |swapn
          |push x
          |swap
          |push int8(14)
          |cast
          |push int32(9)
          |scall
          |concat
          |swap
          |push int8(14)
          |cast
          |push int32(9)
          |scall
          |concat
          |push x00000000
          |concat
          |swap
          |push null
          |jump @_lbl_420
          |@not_emitTokens:
          |@_lbl_98:
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_103:
          |@_lbl_115:
          |pop
          |push x00B6000000000000000000000000000000000000000000000000000000000000
          |push x0400000000000000000000000000000000000000000000000000000000000000
          |dup
          |push int32(5)
          |dupn
          |length
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
          |push int8(9)
          |scall
          |push x2000000000000000000000000000000000000000000000000000000000000000
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |lt
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x008A000000000000000000000000000000000000000000000000000000000000
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_138
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_138:
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
          |push int8(9)
          |scall
          |swap
          |dup
          |dup
          |push int8(4)
          |cast
          |push int32(7)
          |dupn
          |swap
          |dup
          |push int32(32)
          |add
          |swap
          |slice
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
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
          |push int8(9)
          |scall
          |swap
          |push int32(4)
          |swapn
          |push int32(3)
          |swapn
          |swap
          |pop
          |pop
          |pop
          |push x0217000000000000000000000000000000000000000000000000000000000000
          |pop
          |jump @_lbl_535
          |@_lbl_182:
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push bigint(32)
          |swap
          |push int8(4)
          |cast
          |push int32(5)
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
          |push int8(9)
          |scall
          |push int32(3)
          |swapn
          |pop
          |pop
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push bigint(32)
          |swap
          |push int8(4)
          |cast
          |push int32(5)
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
          |push int8(9)
          |scall
          |swap
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(3)
          |dupn
          |push int32(8)
          |scall
          |swap
          |pop
          |swap
          |pop
          |swap
          |jump @convert_result
          |@_lbl_204:
          |@_lbl_216:
          |pop
          |push x011B000000000000000000000000000000000000000000000000000000000000
          |push x0400000000000000000000000000000000000000000000000000000000000000
          |dup
          |push int32(5)
          |dupn
          |length
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
          |push int8(9)
          |scall
          |push x2000000000000000000000000000000000000000000000000000000000000000
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |lt
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x00EF000000000000000000000000000000000000000000000000000000000000
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_239
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_239:
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
          |push int8(9)
          |scall
          |swap
          |dup
          |dup
          |push int8(4)
          |cast
          |push int32(7)
          |dupn
          |swap
          |dup
          |push int32(32)
          |add
          |swap
          |slice
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
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
          |push int8(9)
          |scall
          |swap
          |push int32(4)
          |swapn
          |push int32(3)
          |swapn
          |swap
          |pop
          |pop
          |pop
          |push x022F000000000000000000000000000000000000000000000000000000000000
          |pop
          |jump @_lbl_559
          |@_lbl_283:
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push bigint(32)
          |swap
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
          |push int32(6)
          |dupn
          |push int8(7)
          |scall
          |push int32(5)
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
          |push int8(9)
          |scall
          |push int32(3)
          |swapn
          |pop
          |pop
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push bigint(32)
          |swap
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
          |push int8(9)
          |scall
          |swap
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(3)
          |dupn
          |push int32(8)
          |scall
          |swap
          |pop
          |swap
          |pop
          |swap
          |jump @convert_result
          |@_lbl_305:
          |@_lbl_317:
          |pop
          |push x018A000000000000000000000000000000000000000000000000000000000000
          |push x0400000000000000000000000000000000000000000000000000000000000000
          |dup
          |push int32(5)
          |dupn
          |length
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
          |push int8(9)
          |scall
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |lt
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0154000000000000000000000000000000000000000000000000000000000000
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_340
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_340:
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
          |push int8(9)
          |scall
          |swap
          |dup
          |dup
          |push int8(4)
          |cast
          |push int32(7)
          |dupn
          |swap
          |dup
          |push int32(32)
          |add
          |swap
          |slice
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
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
          |push int8(9)
          |scall
          |swap
          |push int32(4)
          |swapn
          |push int32(3)
          |swapn
          |swap
          |dup
          |push int8(4)
          |cast
          |push int32(8)
          |dupn
          |swap
          |dup
          |push int32(32)
          |add
          |swap
          |slice
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
          |push int8(9)
          |scall
          |swap
          |push int32(4)
          |swapn
          |push int32(3)
          |swapn
          |swap
          |pop
          |pop
          |pop
          |push x0277000000000000000000000000000000000000000000000000000000000000
          |pop
          |jump @_lbl_631
          |@_lbl_394:
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push bigint(32)
          |swap
          |push int8(4)
          |cast
          |push int32(4)
          |dupn
          |push int8(6)
          |scall
          |dup
          |push int32(3)
          |dupn
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(6)
          |dupn
          |push int8(7)
          |scall
          |push int32(5)
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
          |push int8(9)
          |scall
          |push int32(3)
          |swapn
          |pop
          |pop
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push bigint(32)
          |swap
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
          |push int8(9)
          |scall
          |swap
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(3)
          |dupn
          |push int32(8)
          |scall
          |swap
          |pop
          |swap
          |pop
          |swap
          |jump @convert_result
          |@_lbl_420:
          |@_lbl_432:
          |pop
          |push x01FD000000000000000000000000000000000000000000000000000000000000
          |push x0400000000000000000000000000000000000000000000000000000000000000
          |dup
          |push int32(5)
          |dupn
          |length
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
          |push int8(9)
          |scall
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |lt
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x01C7000000000000000000000000000000000000000000000000000000000000
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_455
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_455:
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
          |push int8(9)
          |scall
          |swap
          |dup
          |dup
          |push int8(4)
          |cast
          |push int32(7)
          |dupn
          |swap
          |dup
          |push int32(32)
          |add
          |swap
          |slice
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
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
          |push int8(9)
          |scall
          |swap
          |push int32(4)
          |swapn
          |push int32(3)
          |swapn
          |swap
          |dup
          |push int8(4)
          |cast
          |push int32(8)
          |dupn
          |swap
          |dup
          |push int32(32)
          |add
          |swap
          |slice
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
          |push int8(9)
          |scall
          |swap
          |push int32(4)
          |swapn
          |push int32(3)
          |swapn
          |swap
          |pop
          |pop
          |pop
          |push x0389000000000000000000000000000000000000000000000000000000000000
          |pop
          |jump @_lbl_905
          |@_lbl_509:
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push bigint(32)
          |swap
          |push int8(4)
          |cast
          |push int32(4)
          |dupn
          |push int8(6)
          |scall
          |dup
          |push int32(3)
          |dupn
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |eq
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(6)
          |dupn
          |push int8(7)
          |scall
          |push int32(5)
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
          |push int8(9)
          |scall
          |push int32(3)
          |swapn
          |pop
          |pop
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push bigint(32)
          |swap
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
          |push int8(9)
          |scall
          |swap
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(3)
          |dupn
          |push int32(8)
          |scall
          |swap
          |pop
          |swap
          |pop
          |swap
          |jump @convert_result
          |@_lbl_535:
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push x2000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |push int32(5)
          |dupn
          |push int8(7)
          |scall
          |push int32(4)
          |swapn
          |pop
          |dup
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |push int32(5)
          |dupn
          |push int8(7)
          |scall
          |push int32(4)
          |swapn
          |pop
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(5)
          |dupn
          |push int8(6)
          |scall
          |push int8(10)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int32(3)
          |swapn
          |pop
          |swap
          |pop
          |call @stdlib_evm_sget
          |push int32(2)
          |dupn
          |pop
          |jump @_lbl_182
          |@_lbl_559:
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int32(4)
          |dupn
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(8)
          |dupn
          |push int8(7)
          |scall
          |push int32(7)
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
          |push int8(9)
          |scall
          |swap
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
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(6)
          |dupn
          |push int8(6)
          |scall
          |push int8(10)
          |scall
          |call @stdlib_evm_sget
          |swap
          |pop
          |push int32(3)
          |swapn
          |swap
          |pop
          |pop
          |jump @_lbl_283
          |@_lbl_631:
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int32(2)
          |dupn
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |from
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(10)
          |dupn
          |push int8(7)
          |scall
          |push int32(9)
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
          |push int8(9)
          |scall
          |swap
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(9)
          |dupn
          |push int8(7)
          |scall
          |push int32(8)
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
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(8)
          |dupn
          |push int8(6)
          |scall
          |push int8(10)
          |scall
          |call @stdlib_evm_sget
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
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |from
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(10)
          |dupn
          |push int8(7)
          |scall
          |push int32(9)
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
          |push int8(9)
          |scall
          |swap
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(9)
          |dupn
          |push int8(7)
          |scall
          |push int32(8)
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
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(8)
          |dupn
          |push int8(6)
          |scall
          |push int8(10)
          |scall
          |push int32(2)
          |dupn
          |swap
          |sput
          |pop
          |push int32(2)
          |dupn
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push int32(6)
          |dupn
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(10)
          |dupn
          |push int8(7)
          |scall
          |push int32(9)
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
          |push int8(9)
          |scall
          |swap
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(9)
          |dupn
          |push int8(7)
          |scall
          |push int32(8)
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
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(8)
          |dupn
          |push int8(6)
          |scall
          |push int8(10)
          |scall
          |call @stdlib_evm_sget
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |add
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push int32(6)
          |dupn
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(10)
          |dupn
          |push int8(7)
          |scall
          |push int32(9)
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
          |push int8(9)
          |scall
          |swap
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(9)
          |dupn
          |push int8(7)
          |scall
          |push int32(8)
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
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(8)
          |dupn
          |push int8(6)
          |scall
          |push int8(10)
          |scall
          |push int32(2)
          |dupn
          |swap
          |sput
          |pop
          |push x0100000000000000000000000000000000000000000000000000000000000000
          |swap
          |pop
          |push int32(4)
          |swapn
          |push int32(3)
          |swapn
          |pop
          |pop
          |pop
          |jump @_lbl_394
          |@_lbl_905:
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int32(2)
          |dupn
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push int32(6)
          |dupn
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(10)
          |dupn
          |push int8(7)
          |scall
          |push int32(9)
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
          |push int8(9)
          |scall
          |swap
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(9)
          |dupn
          |push int8(7)
          |scall
          |push int32(8)
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
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(8)
          |dupn
          |push int8(6)
          |scall
          |push int8(10)
          |scall
          |call @stdlib_evm_sget
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |add
          |push int8(14)
          |cast
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push int32(6)
          |dupn
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000
          |and
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(10)
          |dupn
          |push int8(7)
          |scall
          |push int32(9)
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
          |push int8(9)
          |scall
          |swap
          |push int32(2)
          |dupn
          |push int8(4)
          |cast
          |push int32(9)
          |dupn
          |push int8(7)
          |scall
          |push int32(8)
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
          |push int8(9)
          |scall
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |swap
          |push int8(4)
          |cast
          |swap
          |push int32(8)
          |dupn
          |push int8(6)
          |scall
          |push int8(10)
          |scall
          |push int32(2)
          |dupn
          |swap
          |sput
          |pop
          |push x0100000000000000000000000000000000000000000000000000000000000000
          |swap
          |pop
          |push int32(4)
          |swapn
          |push int32(3)
          |swapn
          |pop
          |pop
          |pop
          |jump @_lbl_509
          |push "Invalid"
          |throw
          |@stdlib_evm_sget:
          |dup
          |sexist
          |jumpi @stdlib_evm_sget_non_zero
          |pop
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |ret
          |@stdlib_evm_sget_non_zero:
          |sget
          |ret
          |@convert_result:
          |dup
          |push "emitTokens"
          |eq
          |not
          |jumpi @convert_result_not_emitTokens
          |pop
          |push int8(9)
          |cast
          |stop
          |@convert_result_not_emitTokens:
          |dup
          |push "transfer"
          |eq
          |not
          |jumpi @convert_result_not_transfer
          |pop
          |push int8(9)
          |cast
          |stop
          |@convert_result_not_transfer:
          |dup
          |push "balanceOf"
          |eq
          |not
          |jumpi @convert_result_not_balanceOf
          |pop
          |push int8(4)
          |cast
          |stop
          |@convert_result_not_balanceOf:
          |dup
          |push "balances"
          |eq
          |not
          |jumpi @convert_result_not_balances
          |pop
          |push int8(4)
          |cast
          |stop
          |@convert_result_not_balances:""".stripMargin
      }
    }
  }
}
