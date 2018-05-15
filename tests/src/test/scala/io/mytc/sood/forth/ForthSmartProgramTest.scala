package io.mytc.sood.forth

import ForthTestUtils._
import com.google.protobuf.ByteString
import org.scalatest._
import io.mytc.sood.vm.serialization._

class ForthSmartProgramTest extends FlatSpec with Matchers {
  "Smart-program" should "run correctly" in {
    runProgram[ByteString](
      """
        |1 10 10
        |dup3 1 == if dup1 sget dup3 + dup2 sput then
        |dup3 2 == if dup1 sget dup3 - dup2 sput then
      """.stripMargin
    ) shouldBe Right(
      (
        List(1, 10, 10).map(int32ToData),
        Map(int32ToAddress(10) -> int32ToData(10))
      ))
  }
}
