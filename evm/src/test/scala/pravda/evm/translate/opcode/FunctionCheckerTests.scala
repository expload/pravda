package pravda.evm.translate.opcode

import pravda.evm.EVM._
import pravda.evm.abi.parse.ABIParser
import pravda.evm.{Preconditions, evm}
import pravda.evm.translate.Translator
import utest._
import pravda.evm.evm._
import fastparse.byte.all._
import pravda.vm.Error.UserError

import scala.collection.mutable.ArrayBuffer

object FunctionCheckerTests extends TestSuite {

  val tests = Tests {
    import pravda.vm.Data.Primitive._

    val `0` = hex"0x0"
    val `1` = hex"0x1"
    val `3` = hex"0x3"
    val `4` = hex"0x4"

    "Func call define" - {
      val abi = evm.readSolidityABI("ABIExampleWithOverloading.json")
      val parsedAbi = ABIParser.getContract(abi)
      parsedAbi.isRight ==> true

      parsedAbi.map({
        abis =>
          val opcodes = List(
            0 -> Push(`4`),
            2 -> Push(`0`),
            3 -> CodeCopy,
            4 -> Push(hex"0x04"),
            5 -> CallDataSize,
            6 -> Lt,
            7 -> Push(hex"0x7"),
            8 -> JumpI,
            9 -> Push(hex"0x00"),
            10 -> CallDataLoad,
            11 -> Push(hex"0x0100000000000000000000000000000000000000000000000000000000"),
            12 -> Swap(1),
            13 -> Div,
            14 -> Push(hex"0xffffffff"),
            15 -> And,
            16 -> Dup(1),
            17 -> Push(hex"0x60fe47b1"),
            18 -> Eq,
            19 -> Push(hex"0x8"),
            20 -> JumpI,
            21 -> Dup(1),
            22 -> Push(hex"0x6d4ce63c"),
            23 -> Eq,
            24 -> Push(hex"0x9"),
            25 -> JumpI,
            21 -> Dup(1),
            27 -> Push(hex"0xe5c19b2d"),
            27 -> Eq,
            27 -> Push(hex"0xa"),
            27 -> JumpI,
            11 -> JumpDest,
            27 -> Push(hex"0x00"),
            27 -> Push(`0`),
            27 -> Push(`0`),
            28 -> Dup(1),
            29 -> Revert,
            12 -> JumpDest,
            13 -> Pop,
            27 -> Push(`4`),
            27 -> Push(`4`),
            27 -> Push(`4`),
            27 -> Stop,
            13 -> JumpDest,
            13 -> Pop,
            27 -> Push(`1`),
            27 -> Push(`1`),
            27 -> Push(`1`),
            27 -> Stop,
            14 -> JumpDest,
            15 -> Pop,
            27 -> Push(`3`),
            27 -> Push(`3`),
            27 -> Push(`3`),
            27 -> Stop,
          )

          val precondition = Preconditions(balances = Map.empty, stack = Seq(Utf8("set")), `watts-limit` = 1000L)

          run(Translator.translateActualContract(opcodes, abis), precondition) ==>
            Right(
              expectations(283L,
                           stack =
                             ArrayBuffer(BigInt(scala.BigInt(4)), BigInt(scala.BigInt(4)), BigInt(scala.BigInt(4)))))

          run(Translator.translateActualContract(opcodes, abis), precondition.copy(stack = Seq(Utf8("get")))) ==>
            Right(
              expectations(274L,
                           stack =
                             ArrayBuffer(BigInt(scala.BigInt(1)), BigInt(scala.BigInt(1)), BigInt(scala.BigInt(1)))))

          run(Translator.translateActualContract(opcodes, abis), precondition.copy(stack = Seq(Utf8("gegdft")))) ==>
            Right(
              expectations(218L,
                           stack = ArrayBuffer(Utf8("gegdft")),
                           error = Some(UserError("incorrect function name")))
            )

          run(Translator.translateActualContract(opcodes, abis), precondition.copy(stack = Seq(Utf8("set0")))) ==>
            Right(
              expectations(265L,
                           stack =
                             ArrayBuffer(BigInt(scala.BigInt(3)), BigInt(scala.BigInt(3)), BigInt(scala.BigInt(3)))))
      })

    }

  }

}
