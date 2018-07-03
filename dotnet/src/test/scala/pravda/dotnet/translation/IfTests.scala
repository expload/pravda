package pravda.dotnet.translation

import pravda.common.DiffUtils
import pravda.dotnet.parsers.FileParser
import pravda.vm.asm.PravdaAssembler
import utest._

object IfTests extends TestSuite {

  val tests = Tests {
    'ifTranslation - {
      val Right((_, cilData, methods, signatures)) = FileParser.parseFile("if.exe")

      DiffUtils.assertEqual(
        Translator.translateAsm(methods, cilData, signatures),
        PravdaAssembler.parse(
          """
        |meta method { int8(-1): "Main", int8(-2): int8(0) }
        |dup
        |push "Main"
        |eq
        |jumpi @method_Main
        |jump @stop
        |@method_Main:
        |push int32(0)
        |push int32(0)
        |push int32(0)
        |push int32(0)
        |push int32(0)
        |push int32(0)
        |push int32(0)
        |push int32(0)
        |
        |push int32(10)
        |push int32(9)
        |swapn
        |pop
        |push int32(8)
        |dupn
        |push int32(1)
        |lt
        |push int8(1)
        |cast
        |push int32(8)
        |swapn
        |pop
        |push int32(7)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(1)
        |cast
        |push int32(1)
        |eq
        |jumpi @br16
        |
        |push int32(4)
        |push int32(9)
        |swapn
        |pop
        |
        |@br16:
        |push int32(8)
        |dupn
        |push int32(5)
        |gt
        |push int8(1)
        |cast
        |push int32(7)
        |swapn
        |pop
        |push int32(6)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(1)
        |cast
        |push int32(1)
        |eq
        |jumpi @br38
        |
        |push int32(8)
        |dupn
        |push int32(6)
        |gt
        |push int8(1)
        |cast
        |push int32(6)
        |swapn
        |pop
        |push int32(5)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(1)
        |cast
        |push int32(1)
        |eq
        |jumpi @br37
        |
        |push int32(7)
        |push int32(9)
        |swapn
        |pop
        |
        |@br37:
        |
        |@br38:
        |push int32(8)
        |dupn
        |push int32(0)
        |gt
        |push int8(1)
        |cast
        |push int32(5)
        |swapn
        |pop
        |push int32(4)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(1)
        |cast
        |push int32(1)
        |eq
        |jumpi @br54
        |
        |push int32(4)
        |push int32(9)
        |swapn
        |pop
        |
        |jump @br58
        |@br54:
        |
        |push int32(5)
        |push int32(9)
        |swapn
        |pop
        |
        |@br58:
        |push int32(8)
        |dupn
        |push int32(2)
        |gt
        |push int8(1)
        |cast
        |push int8(9)
        |cast
        |not
        |push int8(1)
        |cast
        |push int32(1)
        |eq
        |jumpi @br68
        |push int32(8)
        |dupn
        |push int32(4)
        |lt
        |push int8(1)
        |cast
        |jump @br69
        |@br68:
        |push int32(0)
        |@br69:
        |push int32(4)
        |swapn
        |pop
        |push int32(3)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(1)
        |cast
        |push int32(1)
        |eq
        |jumpi @br81
        |
        |push int32(6)
        |push int32(9)
        |swapn
        |pop
        |
        |jump @br85
        |@br81:
        |
        |push int32(8)
        |push int32(9)
        |swapn
        |pop
        |
        |@br85:
        |push int32(8)
        |dupn
        |push int32(7)
        |gt
        |push int8(1)
        |cast
        |push int32(1)
        |eq
        |jumpi @br96
        |push int32(8)
        |dupn
        |push int32(10)
        |gt
        |push int8(1)
        |cast
        |jump @br97
        |@br96:
        |push int32(1)
        |@br97:
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
        |jumpi @br109
        |
        |push int32(1)
        |push int32(9)
        |swapn
        |pop
        |
        |jump @br113
        |@br109:
        |
        |push int32(0)
        |push int32(9)
        |swapn
        |pop
        |
        |@br113:
        |push int32(8)
        |dupn
        |push int32(1)
        |gt
        |push int8(1)
        |cast
        |push int8(9)
        |cast
        |not
        |push int8(1)
        |cast
        |push int32(1)
        |eq
        |jumpi @br121
        |push int32(8)
        |dupn
        |push int32(3)
        |lt
        |push int8(1)
        |cast
        |push int32(1)
        |eq
        |jumpi @br128
        |@br121:
        |push int32(8)
        |dupn
        |push int32(20)
        |gt
        |push int8(1)
        |cast
        |jump @br129
        |@br128:
        |push int32(1)
        |@br129:
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
        |jumpi @br141
        |
        |push int32(2)
        |push int32(9)
        |swapn
        |pop
        |
        |jump @br145
        |@br141:
        |
        |push int32(3)
        |push int32(9)
        |swapn
        |pop
        |
        |@br145:
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |jump @stop
        |@stop:
        |
      """.stripMargin
        ).map(_.toList)
      )
    }
  }
}
