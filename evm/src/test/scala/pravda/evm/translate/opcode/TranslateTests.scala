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
          |push int32(3)
          |swapn
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
          |push int32(3)
          |swapn
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
          |push int8(4)
          |cast
          |push int32(3)
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
          |push int8(4)
          |cast
          |push int32(3)
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
          |stop
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
          |pop
          |pop
          |stop""".stripMargin
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
          |push int32(3)
          |swapn
          |jump @_lbl_97
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
          |push int32(3)
          |swapn
          |jump @_lbl_198
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
          |push int32(3)
          |swapn
          |jump @_lbl_299
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
          |push int32(3)
          |swapn
          |jump @_lbl_414
          |@not_emitTokens:
          |@_lbl_92:
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_97:
          |@_lbl_109:
          |pop
          |push x00B0000000000000000000000000000000000000000000000000000000000000
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
          |lt
          |push int8(4)
          |cast
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
          |push x0084000000000000000000000000000000000000000000000000000000000000
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_132
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_132:
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
          |push x0211000000000000000000000000000000000000000000000000000000000000
          |pop
          |jump @_lbl_529
          |@_lbl_176:
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
          |push int8(9)
          |scall
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
          |stop
          |pop
          |stop
          |@_lbl_198:
          |@_lbl_210:
          |pop
          |push x0115000000000000000000000000000000000000000000000000000000000000
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
          |lt
          |push int8(4)
          |cast
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
          |push x00E9000000000000000000000000000000000000000000000000000000000000
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_233
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_233:
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
          |push x0229000000000000000000000000000000000000000000000000000000000000
          |pop
          |jump @_lbl_553
          |@_lbl_277:
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |push int32(3)
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
          |push int8(4)
          |cast
          |push int32(3)
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
          |stop
          |pop
          |stop
          |@_lbl_299:
          |@_lbl_311:
          |pop
          |push x0184000000000000000000000000000000000000000000000000000000000000
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
          |lt
          |push int8(4)
          |cast
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
          |push x014E000000000000000000000000000000000000000000000000000000000000
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_334
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_334:
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
          |push x0271000000000000000000000000000000000000000000000000000000000000
          |pop
          |jump @_lbl_625
          |@_lbl_388:
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |push int32(3)
          |dupn
          |push int8(6)
          |scall
          |dup
          |push int32(3)
          |dupn
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
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
          |push int8(4)
          |cast
          |push int32(3)
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
          |stop
          |pop
          |stop
          |@_lbl_414:
          |@_lbl_426:
          |pop
          |push x01F7000000000000000000000000000000000000000000000000000000000000
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
          |lt
          |push int8(4)
          |cast
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
          |push x01C1000000000000000000000000000000000000000000000000000000000000
          |pop
          |push int8(9)
          |cast
          |jumpi @_lbl_449
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |dup
          |push "Revert"
          |throw
          |@_lbl_449:
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
          |push x0383000000000000000000000000000000000000000000000000000000000000
          |pop
          |jump @_lbl_899
          |@_lbl_503:
          |push x4000000000000000000000000000000000000000000000000000000000000000
          |push int8(4)
          |cast
          |push int32(3)
          |dupn
          |push int8(6)
          |scall
          |dup
          |push int32(3)
          |dupn
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
          |push bigint(0)
          |eq
          |push int8(4)
          |cast
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
          |push int8(4)
          |cast
          |push int32(3)
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
          |stop
          |pop
          |stop
          |@_lbl_529:
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
          |sget
          |push int32(2)
          |dupn
          |pop
          |jump @_lbl_176
          |@_lbl_553:
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
          |push int32(6)
          |dupn
          |push int8(6)
          |scall
          |push int8(10)
          |scall
          |sget
          |swap
          |pop
          |push int32(3)
          |swapn
          |swap
          |pop
          |pop
          |jump @_lbl_277
          |@_lbl_625:
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
          |push int32(8)
          |dupn
          |push int8(6)
          |scall
          |push int8(10)
          |scall
          |sget
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
          |push int32(8)
          |dupn
          |push int8(6)
          |scall
          |push int8(10)
          |scall
          |sget
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
          |jump @_lbl_388
          |@_lbl_899:
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
          |push int32(8)
          |dupn
          |push int8(6)
          |scall
          |push int8(10)
          |scall
          |sget
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
          |jump @_lbl_503
          |stop""".stripMargin
      }
    }
  }
}
