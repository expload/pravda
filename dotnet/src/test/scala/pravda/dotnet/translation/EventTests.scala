package pravda.dotnet

package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object EventTests extends TestSuite {

  val tests = Tests {
    'systemTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("event.exe")

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
            |push "MakeEvent"
            |eq
            |jumpi @method_MakeEvent
            |push "Wrong method name"
            |throw
            |meta translator_mark "MakeEvent method"
            |meta method {
            |"name":"MakeEvent","returnTpe":int8(0)
            |}
            |@method_MakeEvent:
            |meta translator_mark "MakeEvent local vars definition"
            |meta translator_mark "MakeEvent method body"
            |push "my_event"
            |push int32(1234)
            |swap
            |event
            |push "my_event"
            |push "my_string"
            |swap
            |event
            |push "my_event"
            |push number(2.0)
            |swap
            |event
            |push "my_event"
            |new int8[1, 2, 3, 4]
            |call @array_to_bytes
            |swap
            |event
            |meta translator_mark "MakeEvent local vars clearing"
            |pop
            |meta translator_mark "end of MakeEvent method"
            |jump @stop
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
