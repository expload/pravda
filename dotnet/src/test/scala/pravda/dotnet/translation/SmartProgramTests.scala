package pravda.dotnet
package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object SmartProgramTests extends TestSuite {

  val tests = Tests {
    'smartProgramTranslation - {
      val Right((_, cilData, methods, signatures)) = parseFile("smart_program.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler
          .parse("""
            |meta method { int8(-1): "balanceOf", int8(-2): int8(3), int8(0): int8(14) }
            |meta method { int8(-1): "transfer", int8(-2): int8(0), int8(0): int8(14), int8(2): int8(3) }
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
            |push x62616C616E636573
            |push int32(4)
            |dupn
            |push int32(0)
            |call @storage_get_default
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
            |push x62616C616E636573
            |from
            |push int32(0)
            |call @storage_get_default
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
            |push x62616C616E636573
            |from
            |push x62616C616E636573
            |from
            |push int32(0)
            |call @storage_get_default
            |push int32(7)
            |dupn
            |push int32(-1)
            |mul
            |add
            |push int32(2)
            |dupn
            |push int8(14)
            |cast
            |push int32(4)
            |dupn
            |concat
            |swap
            |sput
            |pop
            |pop
            |push x62616C616E636573
            |push int32(6)
            |dupn
            |push x62616C616E636573
            |push int32(8)
            |dupn
            |push int32(0)
            |call @storage_get_default
            |push int32(7)
            |dupn
            |add
            |push int32(2)
            |dupn
            |push int8(14)
            |cast
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
            |@storage_get_default:
            |push int32(2)
            |dupn
            |push int8(14)
            |cast
            |push int32(4)
            |dupn
            |concat
            |sexist
            |jumpi @get_default_if
            |swap
            |pop
            |swap
            |pop
            |ret
            |@get_default_if:
            |pop
            |push int8(14)
            |cast
            |swap
            |concat
            |sget
            |ret
            |@stop:
          """.stripMargin)
          .right
          .get
      )
    }
  }
}
