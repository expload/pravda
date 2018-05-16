package io.mytc.sood.forth

import ForthTestUtils._
import com.google.protobuf.ByteString
import org.scalatest._
import io.mytc.sood.vm.serialization._

class ForthSmartProgramTest extends FlatSpec with Matchers {
  "Simple smart-program" should "run correctly" in {
    runProgram[ByteString](
      """
        |1 10 10
        |dup3 1 == if dup1 sget dup3 + dup2 sput then
        |dup3 2 == if dup1 sget dup3 - dup2 sput then
      """.stripMargin,
      Seq(int32ToAddress(10) -> int32ToData(0))
    ) shouldBe Right(
      (
        List(1, 10, 10).map(int32ToData),
        Map(int32ToAddress(10) -> int32ToData(10))
      ))
  }

  "ERC20 program" should "run correctly" in {
    // arg1 arg2 arg3 ... mode
    // mode == 1 => balanceOf owner
    // mode == 2 => transfer to tokens
    // mode == 3 => transferFrom from to tokens
    // mode == 4 => approve spender tokens
    // mode == 5 => allowance tokenOwner spender
    val erc20 =
      """
        |: balance $xBA concat ;
        |: allowed concat $xA1 concat  ;
        |dup1 1 == if dup2 balance sget then
        |dup1 2 == if from balance sget dup3 <= if
        |                  from balance dup1 sget dup4 - sput
        |                  dup3 balance dup1 sget dup4 + sput
        |          then then
        |dup1 3 == if dup4 balance sget dup3 <= if
        |             dup4 from allowed sget dup3 <= if
        |                  dup4 balance dup1 sget dup4 - sput
        |                  dup4 from allowed dup1 sget dup4 - sput
        |                  dup3 balance dup1 sget dup4 + sput
        |          then then then
        |dup1 4 == if from dup4 allowed dup3 sput then
        |dup1 5 == if dup3 dup3 allowed sget then
      """.stripMargin

    runProgram[ByteString](s"10 1 $erc20".stripMargin, Seq(int32ToAddress(10) -> int32ToData(100))) shouldBe Right(
      (
        Seq(int32ToData(100)),
        Map(int32ToAddress(10) -> int32ToData(100))
      ))
  }
}
