package pravda.dotnet
package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object IfTests extends TestSuite {

  val tests = Tests {
    'ifTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("if.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse(
          """
        |meta translator_mark "jump to methods"
        |meta method {
        |"name":"ifs","returnTpe":int8(0)
        |}
        |dup
        |push "ifs"
        |eq
        |jumpi @method_ifs
        |jump @stop
        |meta translator_mark "ifs method"
        |@method_ifs:
        |meta translator_mark "ifs local vars definition"
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |meta translator_mark "ifs method body"
        |push int32(10)
        |push int32(9)
        |swapn
        |pop
        |push int32(8)
        |dupn
        |push int32(1)
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(8)
        |swapn
        |pop
        |push int32(7)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @ifs_br16
        |push int32(4)
        |push int32(9)
        |swapn
        |pop
        |@ifs_br16:
        |push int32(8)
        |dupn
        |push int32(5)
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(7)
        |swapn
        |pop
        |push int32(6)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @ifs_br38
        |push int32(8)
        |dupn
        |push int32(6)
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(6)
        |swapn
        |pop
        |push int32(5)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @ifs_br37
        |push int32(7)
        |push int32(9)
        |swapn
        |pop
        |@ifs_br37:
        |@ifs_br38:
        |push int32(8)
        |dupn
        |push int32(0)
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(5)
        |swapn
        |pop
        |push int32(4)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @ifs_br54
        |push int32(4)
        |push int32(9)
        |swapn
        |pop
        |jump @ifs_br58
        |@ifs_br54:
        |push int32(5)
        |push int32(9)
        |swapn
        |pop
        |@ifs_br58:
        |push int32(8)
        |dupn
        |push int32(2)
        |swap
        |gt
        |push int8(3)
        |cast
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @ifs_br68
        |push int32(8)
        |dupn
        |push int32(4)
        |swap
        |lt
        |push int8(3)
        |cast
        |jump @ifs_br69
        |@ifs_br68:
        |push int32(0)
        |@ifs_br69:
        |push int32(4)
        |swapn
        |pop
        |push int32(3)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @ifs_br81
        |push int32(6)
        |push int32(9)
        |swapn
        |pop
        |jump @ifs_br85
        |@ifs_br81:
        |push int32(8)
        |push int32(9)
        |swapn
        |pop
        |@ifs_br85:
        |push int32(8)
        |dupn
        |push int32(7)
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @ifs_br96
        |push int32(8)
        |dupn
        |push int32(10)
        |swap
        |gt
        |push int8(3)
        |cast
        |jump @ifs_br97
        |@ifs_br96:
        |push int32(1)
        |@ifs_br97:
        |push int32(3)
        |swapn
        |pop
        |push int32(2)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @ifs_br109
        |push int32(1)
        |push int32(9)
        |swapn
        |pop
        |jump @ifs_br113
        |@ifs_br109:
        |push int32(0)
        |push int32(9)
        |swapn
        |pop
        |@ifs_br113:
        |push int32(8)
        |dupn
        |push int32(1)
        |swap
        |gt
        |push int8(3)
        |cast
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @ifs_br121
        |push int32(8)
        |dupn
        |push int32(3)
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @ifs_br128
        |@ifs_br121:
        |push int32(8)
        |dupn
        |push int32(20)
        |swap
        |gt
        |push int8(3)
        |cast
        |jump @ifs_br129
        |@ifs_br128:
        |push int32(1)
        |@ifs_br129:
        |push int32(2)
        |swapn
        |pop
        |push int32(1)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @ifs_br141
        |push int32(2)
        |push int32(9)
        |swapn
        |pop
        |jump @ifs_br145
        |@ifs_br141:
        |push int32(3)
        |push int32(9)
        |swapn
        |pop
        |@ifs_br145:
        |meta translator_mark "ifs local vars clearing"
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |meta translator_mark "end of ifs method"
        |jump @stop
        |meta translator_mark "ctor method"
        |@method_ctor:
        |meta translator_mark "ctor local vars definition"
        |meta translator_mark "ctor method body"
        |meta translator_mark "ctor local vars clearing"
        |meta translator_mark "end of ctor method"
        |ret
        |meta translator_mark "helper functions"
        |@stop:
      """.stripMargin
        ).right.get
      )
    }
  }
}
