package pravda.dotnet

package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object ObjectsTests extends TestSuite {

  val tests = Tests {
    'objectsTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("objects.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse("""
        |meta translator_mark "jump to methods"
        |push "init"
        |sexist
        |jumpi @methods
        |push "Program was not initialized"
        |throw
        |@methods:
        |dup
        |push "Func"
        |eq
        |jumpi @method_Func
        |dup
        |push "ctor"
        |eq
        |jumpi @method_ctor
        |push "Wrong method name"
        |throw
        |meta translator_mark "Func method"
        |meta method {
        |  "name": "Func", "returnTpe":int8(3)
        |}
        |@method_Func:
        |meta translator_mark "Func local vars definition"
        |push null
        |push null
        |push null
        |push null
        |meta translator_mark "Func method body"
        |push int32(100)
        |new {}
        |call @vtable_A
        |push int32(2)
        |swapn
        |call @func_A.ctor_int32
        |push int32(5)
        |swapn
        |pop
        |push int32(200)
        |new {}
        |call @vtable_B
        |push int32(2)
        |swapn
        |call @func_B.ctor_int32
        |push int32(4)
        |swapn
        |pop
        |push int32(4)
        |dupn
        |call @func_A.AnswerA
        |swap
        |pop
        |push int32(4)
        |dupn
        |call @func_B.AnswerB
        |swap
        |pop
        |add
        |push int32(3)
        |swapn
        |pop
        |push int32(2)
        |dupn
        |push int32(2)
        |swapn
        |pop
        |push int32(1)
        |dupn
        |meta translator_mark "Func local vars clearing"
        |swap
        |pop
        |swap
        |pop
        |swap
        |pop
        |swap
        |pop
        |swap
        |pop
        |meta translator_mark "end of Func method"
        |jump @stop
        |meta translator_mark "ctor method"
        |@method_ctor:
        |meta translator_mark "ctor check"
        |from
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
        |push "init"
        |push null
        |sput
        |meta translator_mark "ctor local vars definition"
        |meta translator_mark "ctor method body"
        |meta translator_mark "ctor local vars clearing"
        |meta translator_mark "end of ctor method"
        |ret
        |meta translator_mark "A.AnswerA func"
        |@func_A.AnswerA:
        |meta translator_mark "A.AnswerA local vars definition"
        |push null
        |meta translator_mark "A.AnswerA func body"
        |push int32(2)
        |dupn
        |struct_get "AVal"
        |push int32(42)
        |add
        |push int32(2)
        |swapn
        |pop
        |push int32(1)
        |dupn
        |meta translator_mark "A.AnswerA local vars clearing"
        |swap
        |pop
        |meta translator_mark "end of A.AnswerA func"
        |ret
        |meta translator_mark "A.ctor_int32 func"
        |@func_A.ctor_int32:
        |meta translator_mark "A.ctor_int32 local vars definition"
        |meta translator_mark "A.ctor_int32 func body"
        |push int32(2)
        |dupn
        |pop
        |push int32(2)
        |dupn
        |push int32(2)
        |dupn
        |struct_mut "AVal"
        |meta translator_mark "A.ctor_int32 local vars clearing"
        |pop
        |meta translator_mark "end of A.ctor_int32 func"
        |ret
        |meta translator_mark "B.AnswerB func"
        |@func_B.AnswerB:
        |meta translator_mark "B.AnswerB local vars definition"
        |push null
        |meta translator_mark "B.AnswerB func body"
        |push int32(2)
        |dupn
        |struct_get "BVal"
        |push int32(43)
        |add
        |push int32(2)
        |swapn
        |pop
        |push int32(1)
        |dupn
        |meta translator_mark "B.AnswerB local vars clearing"
        |swap
        |pop
        |meta translator_mark "end of B.AnswerB func"
        |ret
        |meta translator_mark "B.ctor_int32 func"
        |@func_B.ctor_int32:
        |meta translator_mark "B.ctor_int32 local vars definition"
        |meta translator_mark "B.ctor_int32 func body"
        |push int32(2)
        |dupn
        |pop
        |push int32(2)
        |dupn
        |push int32(2)
        |dupn
        |struct_mut "BVal"
        |meta translator_mark "B.ctor_int32 local vars clearing"
        |pop
        |meta translator_mark "end of B.ctor_int32 func"
        |ret
        |meta translator_mark "A vtable initialization"
        |@vtable_A:
        |ret
        |meta translator_mark "B vtable initialization"
        |@vtable_B:
        |ret
        |meta translator_mark "helper functions"
        |@stop:
      """.stripMargin).right.get
      )
    }
  }
}
