package pravda.forth

import com.google.protobuf.ByteString
import pravda.forth.ForthTestUtils._
import pravda.vm.serialization._
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

    'storage_pcall - {
      val Right(code) = Compiler().compile(
        """
          |dup1 1 == if dup3 dup3 sput then
          |dup1 2 == if dup2 sget then
        """.stripMargin,
        useStdLib = true
      )

      val res = runWithEnviroment[ByteString](
        s"""
           |$$x${code.map("%02X".format(_)).mkString}
           |pcreate
           |10 100 1
           |dup4 3 pcall
           |pop pop pop
           |10 2
           |dup3 2 pcall
          """.stripMargin,
        int32ToByteString(42)
      )

      res ==> Right(
        (
          Seq(1, 10, 2, 100).map(int32ToData),
          Map(int32ToAddress(1) -> ((bytesToByteString(code.map(_.toInt): _*), Map(int32ToAddress(10) -> int32ToData(100)))))
        )
      )
    }
  }
}
