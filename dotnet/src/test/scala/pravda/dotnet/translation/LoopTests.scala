package pravda.dotnet.translation

import pravda.dotnet.DiffUtils
import pravda.dotnet.parsers.FileParser
import pravda.vm.asm.Datum._
import pravda.vm.asm.Op._
import pravda.common.bytes.hex._
import utest._

object LoopTests extends TestSuite {

  val tests = Tests {
    'loopTranslation - {
      val Right((_, cilData, methods, signatures)) = FileParser.parseFile("loop.exe")

      DiffUtils.assertEqual(
        Translator.translateAsm(methods, cilData, signatures),
        Right(
          List(
            Dup,
            Push(Rawbytes("Main".getBytes)),
            Eq,
            JumpI("method_Main"),
            Jump("stop"),
            Label("method_Main"),
            Push(Integral(0)),
            Push(Integral(0)),
            Push(Integral(0)),
            Push(Integral(0)),
            Nop,
            Push(Rawbytes(hex"01 00 00 00 00")),
            Push(Integral(5)),
            SwapN,
            Pop,
            Push(Rawbytes(hex"01 00 00 00 00")),
            Push(Integral(4)),
            SwapN,
            Pop,
            Jump("br17"),
            Label("br7"),
            Nop,
            Push(Integral(4)),
            Dupn,
            Push(Rawbytes(hex"01 00 00 00 02")),
            LCall("Typed", "typedAdd", 2),
            Push(Integral(5)),
            SwapN,
            Pop,
            Nop,
            Push(Integral(3)),
            Dupn,
            Push(Rawbytes(hex"01 00 00 00 01")),
            LCall("Typed", "typedAdd", 2),
            Push(Integral(4)),
            SwapN,
            Pop,
            Label("br17"),
            Push(Integral(3)),
            Dupn,
            Push(Rawbytes(hex"01 00 00 00 0a")),
            LCall("Typed", "typedClt", 2),
            Push(Integral(3)),
            SwapN,
            Pop,
            Push(Integral(2)),
            Dupn,
            Push(Rawbytes(hex"01 00 00 00 01")),
            Eq,
            JumpI("br7"),
            Jump("br34"),
            Label("br28"),
            Nop,
            Push(Integral(4)),
            Dupn,
            Push(Rawbytes(hex"01 00 00 00 02")),
            LCall("Typed", "typedMul", 2),
            Push(Integral(5)),
            SwapN,
            Pop,
            Nop,
            Label("br34"),
            Push(Integral(4)),
            Dupn,
            Push(Rawbytes(hex"01 00 00 27 10")),
            LCall("Typed", "typedClt", 2),
            Push(Integral(2)),
            SwapN,
            Pop,
            Push(Integral(1)),
            Dupn,
            Push(Rawbytes(hex"01 00 00 00 01")),
            Eq,
            JumpI("br28"),
            Pop,
            Pop,
            Pop,
            Pop,
            Pop,
            Jump("stop"),
            Label("stop")
          ))
      )
    }
  }
}
