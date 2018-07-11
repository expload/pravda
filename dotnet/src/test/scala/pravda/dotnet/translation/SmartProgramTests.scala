package pravda.dotnet.translation

import pravda.common.DiffUtils
import pravda.dotnet.parsers.FileParser
import pravda.vm.asm.PravdaAssembler
import utest._

object SmartProgramTests extends TestSuite {

  val tests = Tests {
    'smartProgramTranslation - {
      val Right((_, cilData, methods, signatures)) = FileParser.parseFile("smart_program.exe")

      DiffUtils.assertEqual(
        Translator.translateAsm(methods, cilData, signatures),
        PravdaAssembler.parse(
          """
            |meta method { int8(-1): "balanceOf", int8(-2): int8(3), int8(0): int8(4) }
            |meta method { int8(-1): "transfer", int8(-2): int8(0), int8(0): int8(4), int8(2): int8(3) }
            |meta method { int8(-1): "Main", int8(-2): int8(0) }
            |dup
            |push "balanceOf"
            |eq
            |jumpi @method_balanceOf
            |dup
            |push "transfer"
            |eq
            |jumpi @method_transfer
            |dup
            |push "Main"
            |eq
            |jumpi @method_Main
            |jump @stop
            |@method_balanceOf:
            |push int32(0)
            |push "balances"
            |push int32(4)
            |dupn
            |push int32(0)
            |call @method_getDefault
            |push int32(2)
            |swapn
            |pop
            |push int32(1)
            |dupn
            |swap
            |pop
            |swap
            |pop
            |swap
            |pop
            |jump @stop
            |@method_transfer:
            |push int32(0)
            |push int32(0)
            |push int32(4)
            |dupn
            |push int32(0)
            |gt
            |push int8(1)
            |cast
            |push int32(3)
            |swapn
            |pop
            |push int32(2)
            |dupn
            |push int8(9)
            |cast
            |not
            |push int8(1)
            |cast
            |push int32(1)
            |eq
            |jumpi @br107
            |push "balances"
            |from
            |push int32(0)
            |call @method_getDefault
            |push int32(5)
            |dupn
            |lt
            |push int8(1)
            |cast
            |push int32(0)
            |eq
            |push int8(1)
            |cast
            |push int32(2)
            |swapn
            |pop
            |push int32(1)
            |dupn
            |push int8(9)
            |cast
            |not
            |push int8(1)
            |cast
            |push int32(1)
            |eq
            |jumpi @br106
            |push "balances"
            |from
            |push "balances"
            |from
            |push int32(0)
            |call @method_getDefault
            |push int32(7)
            |dupn
            |push int32(-1)
            |mul
            |add
            |push int32(2)
            |dupn
            |push int32(4)
            |dupn
            |concat
            |swap
            |sput
            |pop
            |pop
            |push "balances"
            |push int32(6)
            |dupn
            |push "balances"
            |push int32(8)
            |dupn
            |push int32(0)
            |call @method_getDefault
            |push int32(7)
            |dupn
            |add
            |push int32(2)
            |dupn
            |push int32(4)
            |dupn
            |concat
            |swap
            |sput
            |pop
            |pop
            |@br106:
            |@br107:
            |pop
            |pop
            |pop
            |pop
            |pop
            |jump @stop
            |@method_Main:
            |pop
            |jump @stop
            |@stop:
          """.stripMargin).map(_.toList)
      )
    }
  }
}
