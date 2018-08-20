package pravda.dotnet
package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object SystemTests extends TestSuite {

  val tests = Tests {
    'systemTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("system.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse(
          """
            |meta translator_mark "jump to methods"
            |dup
            |push "system"
            |eq
            |jumpi @method_system
            |push "Wrong method name"
            |throw
            |meta translator_mark "system method"
            |meta method {
            |"name":"system","returnTpe":int8(0)
            |}
            |@method_system:
            |meta translator_mark "system local vars definition"
            |push null
            |push null
            |push null
            |push null
            |meta translator_mark "system method body"
            |push x
            |owner
            |push int32(5)
            |swapn
            |pop
            |push x
            |balance
            |push int32(4)
            |swapn
            |pop
            |push x0000000000000000000000000000000000000000000000000000000000000000
            |push int32(3)
            |swapn
            |pop
            |paddr
            |push int32(2)
            |swapn
            |pop
            |meta translator_mark "system local vars clearing"
            |pop
            |pop
            |pop
            |pop
            |pop
            |meta translator_mark "end of system method"
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
          """.stripMargin).right.get
      )
    }
  }
}
