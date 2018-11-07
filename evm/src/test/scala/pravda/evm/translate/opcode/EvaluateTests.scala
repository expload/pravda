package pravda.evm.translate.opcode

import fastparse.byte.all._
import pravda.evm.Preconditions
import pravda.common.domain.Address
import pravda.evm.evm._
import pravda.evm.EVM._
import pravda.evm.abi.parse.ABIParser.ABIFunction
import pravda.evm.translate.Translator
import pravda.vm.Data.Primitive.BigInt
import pravda.vm.Effect.{StorageRead, StorageWrite}
import pravda.vm.Error.UserError
import utest._

import scala.collection.mutable.ArrayBuffer

object EvaluateTests extends TestSuite {

  val tests = Tests {
    //FIXME when memory usage will be fixed. All tests will be incorrect
    import pravda.vm.Data.Primitive._
    val precondition = Preconditions(balances = Map.empty, `watts-limit` = 1000L)
    val abi = List(ABIFunction(true, "", Nil, Nil, true, "", None))
    /* FIXME remove
    "PUSH" - {
      run(evmOpToOps(Push(hex"0x80")), precondition) ==> Right(
        expectations(101L, stack = ArrayBuffer(BigInt(scala.BigInt(128)))))
    }*/

    "DUP" - {
      run(Translator(List(Push(hex"0x80"), Dup(1)), abi), precondition) ==>
        Right(expectations(102L, stack = ArrayBuffer(BigInt(scala.BigInt(128)), BigInt(scala.BigInt(128)))))

      run(Translator(List(Push(hex"0x80"), Push(hex"0x60"), Dup(2)), abi), precondition) ==>
        Right(
          expectations(104L,
                       stack =
                         ArrayBuffer(BigInt(scala.BigInt(128)), BigInt(scala.BigInt(96)), BigInt(scala.BigInt(128)))))
    }

    "SWAP" - {
      run(Translator(List(Push(hex"0x80"), Push(hex"0x60"), Swap(1)), abi), precondition) ==>
        Right(expectations(103L, stack = ArrayBuffer(BigInt(scala.BigInt(96)), BigInt(scala.BigInt(128)))))

      run(Translator(List(Push(hex"0x80"), Push(hex"0x60"), Push(hex"0x40"), Swap(2)), abi), precondition) ==>
        Right(
          expectations(105L,
                       stack =
                         ArrayBuffer(BigInt(scala.BigInt(64)), BigInt(scala.BigInt(96)), BigInt(scala.BigInt(128)))))
    }

    //TODO tests with overflow

    "ADD" - {
      run(Translator(List(Push(hex"0x80"), Push(hex"0x60"), Add), abi), precondition) ==>
        Right(expectations(113L, stack = ArrayBuffer(BigInt(scala.BigInt(224)))))
    }

    "MUL" - {
      run(Translator(List(Push(hex"0x80"), Push(hex"0x60"), Mul), abi), precondition) ==>
        Right(expectations(113L, stack = ArrayBuffer(BigInt(scala.BigInt(12288)))))
    }

    "DIV" - {
      run(Translator(List(Push(hex"0x2"), Push(hex"0x60"), Div), abi), precondition) ==>
        Right(expectations(113L, stack = ArrayBuffer(BigInt(scala.BigInt(48)))))
    }

    "MOD" - {
      run(Translator(List(Push(hex"0x2"), Push(hex"0x60"), Mod), abi), precondition) ==>
        Right(expectations(113L, stack = ArrayBuffer(BigInt(scala.BigInt(0)))))

      run(Translator(List(Push(hex"0x7"), Push(hex"0x60"), Mod), abi), precondition) ==>
        Right(expectations(113L, stack = ArrayBuffer(BigInt(scala.BigInt(5)))))
    }

    "SUB" - {
      run(Translator(List(Push(hex"0x60"), Push(hex"0x80"), Sub), abi), precondition) ==>
        Right(expectations(126L, stack = ArrayBuffer(BigInt(scala.BigInt(32)))))
    }

    "ADDMOD" - {
      run(Translator(List(Push(hex"0x3"), Push(hex"0x80"), Push(hex"0x60"), AddMod), abi), precondition) ==>
        Right(expectations(154L, stack = ArrayBuffer(BigInt(scala.BigInt(2)))))
    }

    "MULMOD" - {
      run(Translator(List(Push(hex"0x5"), Push(hex"0x80"), Push(hex"0x60"), MulMod), abi), precondition) ==>
        Right(expectations(154L, stack = ArrayBuffer(BigInt(scala.BigInt(3)))))
    }

    "OR" - {
      val x = hex"0x01"
      val y = hex"0x02"
      val expectation = scala.BigInt(1, x.toArray) | scala.BigInt(1, y.toArray)
      run(Translator(List(Push(x), Push(y), Or), abi), precondition) ==>
        Right(expectations(108L, stack = ArrayBuffer(BigInt(expectation))))

      run(Translator(List(Push(hex"0x1"), Push(hex"0x3"), Or), abi), precondition) ==>
        Right(expectations(108L, stack = ArrayBuffer(BigInt(scala.BigInt(3)))))
    }

    "AND" - {
      val x = hex"0x01"
      val y = hex"0x02"
      val expectation = scala.BigInt(1, x.toArray) & scala.BigInt(1, y.toArray)

      run(Translator(List(Push(x), Push(y), And), abi), precondition) ==>
        Right(expectations(108L, stack = ArrayBuffer(BigInt(expectation))))
      run(Translator(List(Push(hex"0x1"), Push(hex"0x3"), And), abi), precondition) ==>
        Right(expectations(108L, stack = ArrayBuffer(BigInt(scala.BigInt(1)))))
    }

    "XOR" - {
      val x = hex"0x426"
      val y = hex"0x284"
      val expectation = scala.BigInt(1, x.toArray) ^ scala.BigInt(1, y.toArray)

      run(Translator(List(Push(x), Push(y), Xor), abi), precondition) ==>
        Right(expectations(108L, stack = ArrayBuffer(BigInt(expectation))))
    }

    "BYTE" - {
      val x = hex"0x426"
      var pos = hex"0x1f"

      run(Translator(List(Push(x), Push(pos), Byte), abi), precondition) ==>
        Right(expectations(181L, stack = ArrayBuffer(BigInt(scala.BigInt(x.last.toInt)))))

      pos = hex"0x1e"
      val expectation = x.reverse.tail.head.toInt
      run(Translator(List(Push(x), Push(pos), Byte), abi), precondition) ==>
        Right(expectations(181L, stack = ArrayBuffer(BigInt(scala.BigInt(expectation)))))
    }

    "LT" - {
      val x = hex"0x4"
      val y = hex"0x2"
      run(Translator(List(Push(x), Push(y), Lt), abi), precondition) ==>
        Right(expectations(120L, stack = ArrayBuffer(BigInt(scala.BigInt(1)))))

      run(Translator(List(Push(y), Push(x), Lt), abi), precondition) ==>
        Right(expectations(120L, stack = ArrayBuffer(BigInt(scala.BigInt(0)))))
    }

    "GT" - {
      val x = hex"0x4"
      val y = hex"0x2"
      run(Translator(List(Push(x), Push(y), Gt), abi), precondition) ==>
        Right(expectations(120L, stack = ArrayBuffer(BigInt(scala.BigInt(0)))))

      run(Translator(List(Push(y), Push(x), Gt), abi), precondition) ==>
        Right(expectations(120L, stack = ArrayBuffer(BigInt(scala.BigInt(1)))))
    }

    "EQ" - {
      val x = hex"0x4"
      val y = hex"0x2"
      run(Translator(List(Push(x), Push(y), Eq), abi), precondition) ==>
        Right(expectations(120L, stack = ArrayBuffer(BigInt(scala.BigInt(0)))))

      run(Translator(List(Push(x), Push(x), Eq), abi), precondition) ==>
        Right(expectations(120L, stack = ArrayBuffer(BigInt(scala.BigInt(1)))))
    }

    val `0` = hex"0x0"
    val `1` = hex"0x1"
    val `3` = hex"0x3"
    val `4` = hex"0x4"
    val `10` = hex"0xa"

    "JUMPS" - {

      "One jump" - {
        run(
          Translator.translateActualContract(List(
                                               0 -> Push(`4`),
                                               2 -> Push(`0`),
                                               3 -> CodeCopy,
                                               4 -> Push(`4`),
                                               4 -> Push(`3`),
                                               5 -> Jump,
                                               5 -> Push(`4`),
                                               6 -> Push(`4`),
                                               7 -> Push(`4`),
                                               7 -> JumpDest,
                                             ),
                                             abi),
          precondition
        ).foreach({ expect =>
          expect.`watts-spent` ==> 154L
          expect.stack ==> ArrayBuffer(BigInt(scala.BigInt(4)))
          expect.error ==> None
        })

        run(
          Translator.translateActualContract(List(
                                               0 -> Push(`4`),
                                               2 -> Push(`0`),
                                               3 -> CodeCopy,
                                               4 -> Push(`4`),
                                               5 -> Push(`1`),
                                               6 -> Push(`3`),
                                               5 -> JumpI,
                                               5 -> Push(`4`),
                                               6 -> Push(`4`),
                                               7 -> Push(`4`),
                                               7 -> JumpDest,
                                             ),
                                             abi),
          precondition
        ).foreach({ expect =>
          expect.`watts-spent` ==> 173L
          expect.stack ==> ArrayBuffer(BigInt(scala.BigInt(4)))
          expect.error ==> None
        })

        run(
          Translator.translateActualContract(List(
                                               0 -> Push(`4`),
                                               2 -> Push(`0`),
                                               3 -> CodeCopy,
                                               4 -> Push(`4`),
                                               5 -> Push(`0`),
                                               6 -> Push(`3`),
                                               5 -> JumpI,
                                               5 -> Push(`4`),
                                               6 -> Push(`4`),
                                               7 -> Push(`4`),
                                               7 -> JumpDest,
                                             ),
                                             abi),
          precondition
        ).foreach({ expect =>
          expect.`watts-spent` ==> 142L
          expect.stack ==> ArrayBuffer(BigInt(scala.BigInt(4)),
                                       BigInt(scala.BigInt(4)),
                                       BigInt(scala.BigInt(4)),
                                       BigInt(scala.BigInt(4)))
          expect.error ==> None
        })
      }
      "Several jumps" - {
        run(
          Translator.translateActualContract(
            List(
              0 -> Push(`4`),
              2 -> Push(`0`),
              3 -> CodeCopy,
              4 -> Push(`4`),
              5 -> Push(`1`),
              6 -> Push(`3`),
              5 -> JumpI,
              5 -> Push(`4`),
              6 -> Push(`4`),
              7 -> Push(`4`),
              7 -> JumpDest,
              8 -> Push(`1`),
              9 -> Push(`10`),
              10 -> Jump,
              11 -> Push(`4`),
              12 -> Push(`4`),
              13 -> Push(`4`),
              14 -> JumpDest,
            ),
            abi
          ),
          precondition
        ).foreach({ expect =>
          expect.`watts-spent` ==> 246L
          expect.stack ==> ArrayBuffer(BigInt(scala.BigInt(4)), BigInt(scala.BigInt(1)))
          expect.error ==> None
        })

        run(
          Translator.translateActualContract(
            List(
              0 -> Push(`4`),
              2 -> Push(`0`),
              3 -> CodeCopy,
              4 -> Push(`4`),
              6 -> Push(`3`),
              5 -> Jump,
              5 -> Push(`4`),
              6 -> Push(`4`),
              7 -> Push(`4`),
              7 -> JumpDest,
              8 -> Push(`1`),
              9 -> Push(`10`),
              10 -> Jump,
              11 -> Push(`4`),
              12 -> Push(`4`),
              13 -> Push(`4`),
              14 -> JumpDest,
            ),
            abi
          ),
          precondition
        ).foreach({ expect =>
          expect.`watts-spent` ==> 227L
          expect.stack ==> ArrayBuffer(BigInt(scala.BigInt(4)), BigInt(scala.BigInt(1)))
          expect.error ==> None
        })
      }

      "Jump to bad destination" - {

        run(
          Translator.translateActualContract(List(
                                               0 -> Push(`4`),
                                               2 -> Push(`0`),
                                               3 -> CodeCopy,
                                               4 -> Push(`4`),
                                               4 -> Push(`4`),
                                               5 -> Jump,
                                               5 -> Push(`4`),
                                               6 -> Push(`4`),
                                               7 -> Push(`4`),
                                               7 -> JumpDest,
                                             ),
                                             abi),
          precondition
        ).foreach({ expect =>
          expect.`watts-spent` ==> 147L
          expect.stack ==> ArrayBuffer(BigInt(scala.BigInt(4)), BigInt(scala.BigInt(4)))
          expect.error ==> Some(UserError("Incorrect destination"))
        })

      }
    }

    "SSTORE SLOAD" - {

      run(Translator(List(
                       Push(`4`),
                       Push(`3`),
                       SStore,
                       Push(`3`),
                       SLoad
                     ),
                     abi),
          precondition).foreach({ expect =>
        expect.`watts-spent` ==> 193
        expect.effects ==> ArrayBuffer(
          StorageWrite(Address.Void, BigInt(scala.BigInt(3)), None, BigInt(scala.BigInt(4))),
          StorageRead(Address.Void, BigInt(scala.BigInt(3)), Some(BigInt(scala.BigInt(4))))
        )
        expect.stack ==> ArrayBuffer(BigInt(scala.BigInt(4)))
        expect.error ==> None
      })
    }

  }

}
