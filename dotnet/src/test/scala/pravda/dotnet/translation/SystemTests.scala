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
        PravdaAssembler.parse("""
            |meta translator_mark "jump to methods"
            |dup
            |push "ctor"
            |eq
            |jumpi @method_ctor
            |push "init"
            |sexist
            |jumpi @methods
            |push "Program was not initialized"
            |throw
            |@methods:
            |dup
            |push "system"
            |eq
            |jumpi @method_system
            |push "Wrong method name"
            |throw
            |meta translator_mark "ctor method"
            |meta method {
            |  "name":"ctor","returnTpe":int8(0)
            |}
            |@method_ctor:
            |meta translator_mark "ctor check"
            |from
            |paddr
            |owner
            |eq
            |jumpi @ctor_ok_1
            |push "Only owner can call the constructor"
            |throw
            |@ctor_ok_1:
            |push "init"
            |sexist
            |not
            |jumpi @ctor_ok_2
            |push "Program has been already initialized"
            |throw
            |@ctor_ok_2:
            |push null
            |push "init"
            |sput
            |meta translator_mark "ctor local vars definition"
            |meta translator_mark "ctor method body"
            |meta translator_mark "ctor local vars clearing"
            |pop
            |meta translator_mark "end of ctor method"
            |jump @stop
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
            |meta translator_mark "helper functions"
            |@stop:
          """.stripMargin).right.get
      )
    }
  }
}
