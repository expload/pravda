package pravda.evm.translate.opcode

import fastparse.byte.all._
import pravda.evm._
import pravda.evm.EVM._
import pravda.evm.translate.Translator
import pravda.vm.Data.Primitive.BigInt
import pravda.vm.VmSandbox.EnvironmentEffect.{StorageGet, StoragePut}
import pravda.vm.VmSandbox.Preconditions
import utest._

import scala.collection.mutable.ArrayBuffer

object EvaluateTests extends TestSuite {

  val tests = Tests {
    //FIXME when memory usage will be fixed. All tests will be incorrect
    import SimpleTranslation._
    import pravda.vm.Data.Primitive._
    val precondition = Preconditions(balances = Map.empty, watts = 1000L, executor = None)
    "PUSH" - {
      run(evmOpToOps(Push(hex"0x80")), precondition) ==> Right(
        expectations(101L, stack = ArrayBuffer(BigInt(scala.BigInt(128)))))
    }

    "DUP" - {
      run(Translator(List(Push(hex"0x80"), Dup(1))), precondition) ==>
        Right(expectations(102L, stack = ArrayBuffer(BigInt(scala.BigInt(128)), BigInt(scala.BigInt(128)))))

      run(Translator(List(Push(hex"0x80"), Push(hex"0x60"), Dup(2))), precondition) ==>
        Right(
          expectations(104L,
                       stack =
                         ArrayBuffer(BigInt(scala.BigInt(128)), BigInt(scala.BigInt(96)), BigInt(scala.BigInt(128)))))
    }

    "SWAP" - {
      run(Translator(List(Push(hex"0x80"), Push(hex"0x60"), Swap(1))), precondition) ==>
        Right(expectations(103L, stack = ArrayBuffer(BigInt(scala.BigInt(96)), BigInt(scala.BigInt(128)))))

      run(Translator(List(Push(hex"0x80"), Push(hex"0x60"), Push(hex"0x40"), Swap(2))), precondition) ==>
        Right(
          expectations(105L,
                       stack =
                         ArrayBuffer(BigInt(scala.BigInt(64)), BigInt(scala.BigInt(96)), BigInt(scala.BigInt(128)))))
    }

    //TODO tests with overflow

    "ADD" - {
      run(Translator(List(Push(hex"0x80"), Push(hex"0x60"), Add)), precondition) ==>
        Right(expectations(113L, stack = ArrayBuffer(BigInt(scala.BigInt(224)))))
    }

    "MUL" - {
      run(Translator(List(Push(hex"0x80"), Push(hex"0x60"), Mul)), precondition) ==>
        Right(expectations(113L, stack = ArrayBuffer(BigInt(scala.BigInt(12288)))))
    }

    "DIV" - {
      run(Translator(List(Push(hex"0x2"), Push(hex"0x60"), Div)), precondition) ==>
        Right(expectations(113L, stack = ArrayBuffer(BigInt(scala.BigInt(48)))))
    }

    "MOD" - {
      run(Translator(List(Push(hex"0x2"), Push(hex"0x60"), Mod)), precondition) ==>
        Right(expectations(113L, stack = ArrayBuffer(BigInt(scala.BigInt(0)))))

      run(Translator(List(Push(hex"0x7"), Push(hex"0x60"), Mod)), precondition) ==>
        Right(expectations(113L, stack = ArrayBuffer(BigInt(scala.BigInt(5)))))
    }

    "SUB" - {
      run(Translator(List(Push(hex"0x60"), Push(hex"0x80"), Sub)), precondition) ==>
        Right(expectations(126L, stack = ArrayBuffer(BigInt(scala.BigInt(32)))))
    }

    "ADDMOD" - {
      run(Translator(List(Push(hex"0x3"), Push(hex"0x80"), Push(hex"0x60"), AddMod)), precondition) ==>
        Right(expectations(154L, stack = ArrayBuffer(BigInt(scala.BigInt(2)))))
    }

    "MULMOD" - {
      run(Translator(List(Push(hex"0x5"), Push(hex"0x80"), Push(hex"0x60"), MulMod)), precondition) ==>
        Right(expectations(154L, stack = ArrayBuffer(BigInt(scala.BigInt(3)))))
    }

    "OR" - {
      val x = hex"0x01"
      val y = hex"0x02"
      val expectation = scala.BigInt(1, x.toArray) | scala.BigInt(1, y.toArray)
      run(Translator(List(Push(x), Push(y), Or)), precondition) ==>
        Right(expectations(108L, stack = ArrayBuffer(BigInt(expectation))))

      run(Translator(List(Push(hex"0x1"), Push(hex"0x3"), Or)), precondition) ==>
        Right(expectations(108L, stack = ArrayBuffer(BigInt(scala.BigInt(3)))))
    }

    "AND" - {
      val x = hex"0x01"
      val y = hex"0x02"
      val expectation = scala.BigInt(1, x.toArray) & scala.BigInt(1, y.toArray)

      run(Translator(List(Push(x), Push(y), And)), precondition) ==>
        Right(expectations(108L, stack = ArrayBuffer(BigInt(expectation))))
      run(Translator(List(Push(hex"0x1"), Push(hex"0x3"), And)), precondition) ==>
        Right(expectations(108L, stack = ArrayBuffer(BigInt(scala.BigInt(1)))))
    }

    "XOR" - {
      val x = hex"0x426"
      val y = hex"0x284"
      val expectation = scala.BigInt(1, x.toArray) ^ scala.BigInt(1, y.toArray)

      run(Translator(List(Push(x), Push(y), Xor)), precondition) ==>
        Right(expectations(108L, stack = ArrayBuffer(BigInt(expectation))))
    }

    "BYTE" - {
      val x = hex"0x426"
      var pos = hex"0x1f"

      run(Translator(List(Push(x), Push(pos), Byte)), precondition) ==>
        Right(expectations(191L, stack = ArrayBuffer(BigInt(scala.BigInt(x.last.toInt)))))

      pos = hex"0x1e"
      val expectation = x.reverse.tail.head.toInt
      run(Translator(List(Push(x), Push(pos), Byte)), precondition) ==>
        Right(expectations(191L, stack = ArrayBuffer(BigInt(scala.BigInt(expectation)))))
    }

    "LT" - {
      val x = hex"0x4"
      val y = hex"0x2"
      run(Translator(List(Push(x), Push(y), Lt)), precondition) ==>
        Right(expectations(120L, stack = ArrayBuffer(BigInt(scala.BigInt(1)))))

      run(Translator(List(Push(y), Push(x), Lt)), precondition) ==>
        Right(expectations(120L, stack = ArrayBuffer(BigInt(scala.BigInt(0)))))
    }

    "GT" - {
      val x = hex"0x4"
      val y = hex"0x2"
      run(Translator(List(Push(x), Push(y), Gt)), precondition) ==>
        Right(expectations(120L, stack = ArrayBuffer(BigInt(scala.BigInt(0)))))

      run(Translator(List(Push(y), Push(x), Gt)), precondition) ==>
        Right(expectations(120L, stack = ArrayBuffer(BigInt(scala.BigInt(1)))))
    }

    "EQ" - {
      val x = hex"0x4"
      val y = hex"0x2"
      run(Translator(List(Push(x), Push(y), Eq)), precondition) ==>
        Right(expectations(120L, stack = ArrayBuffer(BigInt(scala.BigInt(0)))))

      run(Translator(List(Push(x), Push(x), Eq)), precondition) ==>
        Right(expectations(120L, stack = ArrayBuffer(BigInt(scala.BigInt(1)))))
    }

    val `0` = hex"0x0"
    val `1` = hex"0x1"
    val `3` = hex"0x3"
    val `4` = hex"0x4"
    "JUMPS" - {

      run(
        Translator.translateActualContract(
          List(
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
          )),
        precondition
      ).foreach({ expect =>
        expect.watts ==> 154L
        expect.memory.stack ==> ArrayBuffer(BigInt(scala.BigInt(4)))
        expect.error ==> None
      })

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
          )),
        precondition
      ).foreach({ expect =>
        expect.watts ==> 173L
        expect.memory.stack ==> ArrayBuffer(BigInt(scala.BigInt(4)))
        expect.error ==> None
      })
    }

    "SSTORE SLOAD" - {

      run(Translator(
            List(
              Push(`4`),
              Push(`3`),
              SStore,
              Push(`3`),
              SLoad
            )
          ),
          precondition).foreach({ expect =>
        expect.watts ==> 193
        expect.effects ==> ArrayBuffer(StoragePut(BigInt(scala.BigInt(3)), BigInt(scala.BigInt(4))),
                                       StorageGet(BigInt(scala.BigInt(3)), Some(BigInt(scala.BigInt(4)))))
        expect.memory.stack ==> ArrayBuffer(BigInt(scala.BigInt(4)))
        expect.error ==> None
      })
    }

  }

}
