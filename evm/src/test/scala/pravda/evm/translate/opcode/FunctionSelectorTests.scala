package pravda.evm

package translate.opcode

import fastparse.byte.all._
import pravda.evm.EVM._
import pravda.evm.abi.parse.AbiParser
import pravda.evm.EvmSandbox
import pravda.evm.disasm.{Blocks, JumpTargetRecognizer}
import pravda.evm.translate.Translator.Addressed
import pravda.vm.Data.Primitive._
import pravda.vm.Error.UserError
import pravda.vm.VmSandbox
import utest._

object FunctionSelectorTests extends TestSuite {

  val tests = Tests {
    import VmSandbox.{ExpectationsWithoutWatts => Expectations}

    val `0` = hex"0x0"
    val `1` = hex"0x1"
    val `3` = hex"0x3"
    val `4` = hex"0x4"

    "Func call define" - {
      val abi = readSolidityABI("ABIExampleWithOverloading.json")
      val Right(abis) = AbiParser.parseAbi(abi)

      val opcodes = List(
        0 -> Push(`4`),
        2 -> Push(`0`),
        3 -> CodeCopy,
        4 -> Push(hex"0x04"),
        5 -> CallDataSize,
        6 -> Lt,
        7 -> Push(hex"0x7"),
        8 -> SelfAddressedJumpI(8),
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
        20 -> SelfAddressedJumpI(20),
        21 -> Dup(1),
        22 -> Push(hex"0x6d4ce63c"),
        23 -> Eq,
        24 -> Push(hex"0x9"),
        25 -> SelfAddressedJumpI(25),
        21 -> Dup(1),
        27 -> Push(hex"0xe5c19b2d"),
        27 -> Eq,
        27 -> Push(hex"0xa"),
        27 -> SelfAddressedJumpI(27),
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
        90 -> Stop,
      )

      val run: (VmSandbox.Preconditions, List[Addressed[Op]]) => Either[String, VmSandbox.ExpectationsWithoutWatts] =
        (precondition, ops) =>
          for {
            code <- Blocks.splitToCreativeAndRuntime(ops)
            code1 <- JumpTargetRecognizer(code._2).left.map(_.toString)
            res <- EvmSandbox.runCode(precondition, code1.map(_._2), abis)
          } yield res

      val precondition = VmSandbox.Preconditions(balances = Map.empty,
                                                 stack = Seq(Utf8("set"), BigInt(scala.BigInt(32))),
                                                 `watts-limit` = 10000L)

      run(precondition, opcodes) ==>
        Right(
          Expectations(stack =
            Seq(BigInt(scala.BigInt(32)), BigInt(scala.BigInt(4)), BigInt(scala.BigInt(4)), BigInt(scala.BigInt(4)))))

      run(precondition.copy(stack = Seq(Utf8("get"), BigInt(scala.BigInt(32)))), opcodes) ==>
        Right(
          Expectations(stack =
            Seq(BigInt(scala.BigInt(32)), BigInt(scala.BigInt(1)), BigInt(scala.BigInt(1)), BigInt(scala.BigInt(1)))))

      run(precondition.copy(stack = Seq(Utf8("gegdft"), BigInt(scala.BigInt(32)))), opcodes) ==>
        Right(
          Expectations(stack = Seq(BigInt(scala.BigInt(32)), Utf8("gegdft")),
                       error = Some(UserError("incorrect function name"))))

      run(precondition.copy(stack = Seq(Utf8("set0"), BigInt(scala.BigInt(32)))), opcodes) ==>
        Right(
          Expectations(stack =
            Seq(BigInt(scala.BigInt(32)), BigInt(scala.BigInt(3)), BigInt(scala.BigInt(3)), BigInt(scala.BigInt(3)))))
    }
  }
}
