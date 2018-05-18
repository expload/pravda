package io.mytc.sood.forth

import utest._
import ForthTestUtils._
import com.google.protobuf.ByteString
import io.mytc.sood.vm.serialization._

object ForthSmartProgramTest extends TestSuite {

  def tests = Tests {
    "Simple smart-program must run correctly" - {
      runProgram[ByteString](
        """
          |1 10 10
          |dup3 1 == if dup1 sget dup3 + dup2 sput then
          |dup3 2 == if dup1 sget dup3 - dup2 sput then
        """.stripMargin,
        Seq(int32ToAddress(10) -> int32ToData(0))
      ) ==> Right(
        (
          List(1, 10, 10).map(int32ToData),
          Map(int32ToAddress(10) -> int32ToData(10))
        ))
    }

    "ERC20 program must run correctly" - {
      // arg1 arg2 arg3 ... mode
      // mode == 1 => balanceOf owner
      // mode == 2 => transfer to tokens
      // mode == 3 => transferFrom from to tokens
      // mode == 4 => approve spender tokens
      // mode == 5 => allowance tokenOwner spender
      val erc20 =
        """
        |: balance $xBA concat ;
        |: allowed concat $xA1 concat ;
        |dup1 0 == if from $x67EA4654C7F00206215A6B32C736E75A77C0B066D9F5CEDD656714F1A8B64A45 == if from balance dup3 sput
        |dup1 1 == if dup2 balance sget then
        |dup1 2 == if dup2 0 < if
        |             from balance sget dup3 <= if
        |                  from balance dup1 sget dup4 - sput
        |                  dup3 balance dup1 sget dup4 + sput
        |          then then then
        |dup1 3 == if dup2 0 < if
        |             dup4 balance sget dup3 <= if
        |             dup4 from allowed sget dup3 <= if
        |                  dup4 balance dup1 sget dup4 - sput
        |                  dup4 from allowed dup1 sget dup4 - sput
        |                  dup3 balance dup1 sget dup4 + sput
        |          then then then then
        |dup1 4 == if from dup4 allowed dup3 sput then
        |dup1 5 == if dup3 dup3 allowed sget then
      """.stripMargin

      val balance10 = bytesToAddress(0xba, 0, 0, 0, 10)
      val balance20 = bytesToAddress(0xba, 0, 0, 0, 20)
      val allowed1030 = bytesToAddress(0xa1, 0, 0, 0, 30, 0, 0, 0, 10)

      'mode1 - {
        runProgram[ByteString](s"10 1 $erc20", Seq(balance10 -> int32ToData(100))) ==> Right(
          (
            Seq(10, 1, 100).map(int32ToData),
            Map(balance10 -> int32ToData(100))
          ))
      }

      'mode2 - {
        runProgram[ByteString](s"20 5 2 $erc20",
          Seq(balance10 -> int32ToData(100), balance20 -> int32ToData(10)),
          int32ToAddress(10)) ==> Right(
          (
            Seq(20, 5, 2).map(int32ToData),
            Map(balance10 -> int32ToData(95), balance20 -> int32ToData(15))
          ))

        runProgram[ByteString](s"20 500 2 $erc20",
          Seq(balance10 -> int32ToData(100), balance20 -> int32ToData(10)),
          int32ToAddress(10)) ==> Right(
          (
            Seq(20, 500, 2).map(int32ToData),
            Map(balance10 -> int32ToData(100), balance20 -> int32ToData(10))
          ))

        runProgram[ByteString](s"20 -10 2 $erc20",
          Seq(balance10 -> int32ToData(100), balance20 -> int32ToData(10)),
          int32ToAddress(10)) ==> Right(
          (
            Seq(20, -10, 2).map(int32ToData),
            Map(balance10 -> int32ToData(100), balance20 -> int32ToData(10))
          ))
      }

      'mode3 - {
        runProgram[ByteString](s"10 20 5 3 $erc20",
          Seq(balance10 -> int32ToData(100),
            balance20 -> int32ToData(10),
            allowed1030 -> int32ToData(10)),
          int32ToAddress(30)) ==> Right(
          (
            Seq(10, 20, 5, 3).map(int32ToData),
            Map(balance10 -> int32ToData(95), balance20 -> int32ToData(15), allowed1030 -> int32ToData(5))
          ))

        runProgram[ByteString](s"10 20 20 3 $erc20",
          Seq(balance10 -> int32ToData(100),
            balance20 -> int32ToData(10),
            allowed1030 -> int32ToData(10)),
          int32ToAddress(30)) ==> Right(
          (
            Seq(10, 20, 20, 3).map(int32ToData),
            Map(balance10 -> int32ToData(100), balance20 -> int32ToData(10), allowed1030 -> int32ToData(10))
          ))

        runProgram[ByteString](s"10 20 200 3 $erc20",
          Seq(balance10 -> int32ToData(100),
            balance20 -> int32ToData(10),
            allowed1030 -> int32ToData(1000)),
          int32ToAddress(30)) ==> Right(
          (
            Seq(10, 20, 200, 3).map(int32ToData),
            Map(balance10 -> int32ToData(100), balance20 -> int32ToData(10), allowed1030 -> int32ToData(1000))
          ))

        runProgram[ByteString](s"10 20 -5 3 $erc20",
          Seq(balance10 -> int32ToData(100),
            balance20 -> int32ToData(10),
            allowed1030 -> int32ToData(10)),
          int32ToAddress(30)) ==> Right(
          (
            Seq(10, 20, -5, 3).map(int32ToData),
            Map(balance10 -> int32ToData(100), balance20 -> int32ToData(10), allowed1030 -> int32ToData(10))
          ))
      }

      'mode4 - {
        runProgram[ByteString](s"30 5 4 $erc20", Seq(allowed1030 -> int32ToData(10)), int32ToAddress(10)) ==> Right(
          (
            Seq(30, 5, 4).map(int32ToData),
            Map(allowed1030 -> int32ToData(5))
          ))
      }

      'mode5 - {
        runProgram[ByteString](s"10 30 5 $erc20", Seq(allowed1030 -> int32ToData(10))) ==> Right(
          (
            Seq(10, 30, 5, 10).map(int32ToData),
            Map(allowed1030 -> int32ToData(10))
          ))
      }
    }
  }
}
