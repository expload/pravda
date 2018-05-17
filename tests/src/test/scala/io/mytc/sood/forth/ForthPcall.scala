package io.mytc.sood.forth

import com.google.protobuf.ByteString
import io.mytc.sood.forth.ForthTestUtils._
import io.mytc.sood.vm.serialization._
import utest._

object ForthPcall extends TestSuite {

  val tests = Tests {
    'simple_pcall - {
      val Right(code) = Compiler().compile(
        "add",
        useStdLib = true
      )

      val res = runWithEnviroment[ByteString](
        s"""
            |10 10
            |$$x${code.map("%02X".format(_)).mkString}
            |pcreate 2 pcall
          """.stripMargin,
        int32ToByteString(42)
      )

      res ==> Right(
        (
          Seq(int32ToData(20)),
          Map(int32ToAddress(1) -> ((bytesToByteString(code.map(_.toInt): _*), Map.empty)))
        )
      )
    }
  }
}
