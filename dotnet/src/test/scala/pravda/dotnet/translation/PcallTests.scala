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
          |push "pcall"
          |eq
          |jumpi @method_pcall
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
          |meta translator_mark "pcall method"
          |meta method {
          |"name":"pcall","returnTpe":int8(3)
          |}
          |@method_pcall:
          |meta translator_mark "pcall local vars definition"
          |push null
          |push null
          |meta translator_mark "pcall method body"
          |push x0000000000000000000000000000000000000000000000000000000000000000
          |push int32(10)
          |push int32(20)
          |push int32(2)
          |swapn
          |push int32(3)
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
          |meta translator_mark "pcall local vars clearing"
          |swap
          |pop
          |swap
          |pop
          |swap
          |pop
          |meta translator_mark "end of pcall method"
          |jump @stop
          |meta translator_mark "helper functions"
          |@stop:
      """.stripMargin).right.get
      )
    }
  }
}
