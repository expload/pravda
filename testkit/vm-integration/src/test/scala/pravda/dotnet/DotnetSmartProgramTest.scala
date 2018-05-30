package pravda.dotnet

import com.google.protobuf.ByteString
import utest._
import pravda.test.utils.IntegrationUtils._
import pravda.vm.asm.{Datum, Op}
import pravda.vm.serialization._

object DotnetSmartProgramTest extends TestSuite {

  val tests = Tests {
    "Smart-program must run correctly" - {
      val Right((_, cilData, methods, signatures)) = FileParser.parsePe("smart_program.exe")
      val program = Translator.translate(methods, cilData, signatures)

      val balance10 = ByteString.copyFrom("balances".getBytes ++ Array[Byte](0, 0, 0, 10))
      val balance20 = ByteString.copyFrom("balances".getBytes ++ Array[Byte](0, 0, 0, 20))

      def pushBytes(bs: ByteString): Op =
        Op.Push(Datum.Rawbytes(bs.toByteArray))

      def typedInt(bs: ByteString): ByteString =
        ByteString.copyFrom(Array[Byte](1)).concat(bs)

      'balanceOf - {
        val args = Seq(int32ToAddress(10), ByteString.copyFrom("balanceOf".getBytes))

        runAsmProgram[ByteString](args.map(pushBytes) ++ program, Seq(balance10 -> typedInt(int32ToData(100)))) ==>
          ((
             Seq(typedInt(int32ToData(100))),
             Map(balance10 -> typedInt(int32ToData(100)))
           ))

        runAsmProgram[ByteString](args.map(pushBytes) ++ program, Seq()) ==>
          ((
             Seq(typedInt(int32ToData(0))),
             Map()
           ))

      }

      'transfer - {
        val args = Seq(int32ToAddress(20), typedInt(int32ToData(5)), ByteString.copyFrom("transfer".getBytes))

        runAsmProgram[ByteString](args.map(pushBytes) ++ program, Seq(balance10 -> typedInt(int32ToData(100)), balance20 -> typedInt(int32ToData(10)))) ==>
          ((
            Seq(),
            Map(balance10 -> typedInt(int32ToData(95)), balance20 -> typedInt(int32ToData(15)))
          ))
//
//        runForthProgram[ByteString](s"20 500 2 $erc20",
//          Seq(balance10 -> int32ToData(100), balance20 -> int32ToData(10)),
//          int32ToAddress(10)) ==> Right(
//          (
//            Seq(20, 500, 2).map(int32ToData),
//            Map(balance10 -> int32ToData(100), balance20 -> int32ToData(10))
//          ))
//
//        runForthProgram[ByteString](s"20 -10 2 $erc20",
//          Seq(balance10 -> int32ToData(100), balance20 -> int32ToData(10)),
//          int32ToAddress(10)) ==> Right(
//          (
//            Seq(20, -10, 2).map(int32ToData),
//            Map(balance10 -> int32ToData(100), balance20 -> int32ToData(10))
//          ))
      }
    }
  }
}
