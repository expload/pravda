package pravda.dotnet
package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object PcallTests extends TestSuite {

  val tests = Tests {
    'pcallTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("pcall.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse("""
          |meta translator_mark "jump to methods"
          |dup
          |push "scall"
          |eq
          |jumpi @method_scall
          |push "Wrong method name"
          |throw
          |meta translator_mark "ctor method"
          |@method_ctor:
          |meta translator_mark "ctor local vars definition"
          |meta translator_mark "ctor method body"
          |meta translator_mark "ctor local vars clearing"
          |meta translator_mark "end of ctor method"
          |ret
          |meta translator_mark "scall method"
          |meta method {
          |"name":"scall","returnTpe":int8(3)
          |}
          |@method_scall:
          |meta translator_mark "scall local vars definition"
          |push null
          |push null
          |meta translator_mark "scall method body"
          |new int8[1, 2, 3, 4]
          |call @array_to_bytes
          |push int32(10)
          |push int32(20)
          |push int32(2)
          |swapn
          |push int32(1)
          |swapn
          |push "Add"
          |swap
          |push int32(3)
          |pcall
          |push int32(4)
          |swapn
          |pop
          |push int32(3)
          |dupn
          |push int32(3)
          |swapn
          |pop
          |push int32(2)
          |dupn
          |meta translator_mark "scall local vars clearing"
          |swap
          |pop
          |swap
          |pop
          |swap
          |pop
          |meta translator_mark "end of scall method"
          |jump @stop
          |meta translator_mark "helper functions"
          |@array_to_bytes:
          |dup
          |length
          |push x
          |push int32(0)
          |@array_to_bytes_loop:
          |push int32(4)
          |dupn
          |push int32(2)
          |dupn
          |array_get
          |push int8(14)
          |cast
          |push int32(3)
          |dupn
          |concat
          |push int32(3)
          |swapn
          |pop
          |push int32(1)
          |add
          |dup
          |push int32(4)
          |dupn
          |gt
          |jumpi @array_to_bytes_loop
          |pop
          |swap
          |pop
          |swap
          |pop
          |ret
          |@stop:
      """.stripMargin).right.get
      )
    }
  }
}
