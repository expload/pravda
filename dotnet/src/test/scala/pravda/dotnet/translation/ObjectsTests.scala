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
        |dup
        |push "Func"
        |eq
        |jumpi @method_Func
        |push "Wrong method name"
        |throw
        |meta translator_mark "Func method"
        |@method_Func:
        |meta translator_mark "Func local vars definition"
        |push null
        |push null
        |push null
        |meta translator_mark "Func method body"
        |push int32(100)
        |call @func_A.ctor
        |push int32(3)
        |swapn
        |pop
        |push int32(200)
        |call @func_B.ctor
        |push int32(1)
        |swapn
        |pop
        |push int32(1)
        |dupn
        |push int32(1)
        |dupn
        |struct_get "AnswerA"
        |call
        |push int32(0)
        |dupn
        |push int32(1)
        |dupn
        |struct_get "AnswerB"
        |call
        |add
        |push int32(-2)
        |swapn
        |pop
        |meta translator_mark "Func local vars clearing"
        |pop
        |pop
        |pop
        |pop
        |meta translator_mark "end of Func method"
        |jump @stop
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
        |swap
        |pop
        |meta translator_mark "end of A.AnswerA func"
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
        |swap
        |pop
        |meta translator_mark "end of B.AnswerB func"
        |ret
        |meta translator_mark "A.ctor func"
        |@func_A.ctor:
        |meta translator_mark "A.ctor func prefix"
        |new {}
        |dup
        |push @A.AnswerA
        |struct_mut "AnswerA"
        |push int32(2)
        |swapn
        |meta translator_mark "A.ctor local vars definition"
        |meta translator_mark "A.ctor func body"
        |push int32(2)
        |dupn
        |pop
        |push int32(2)
        |dupn
        |push int32(2)
        |dupn
        |struct_mut "AVal"
        |meta translator_mark "A.ctor local vars clearing"
        |pop
        |meta translator_mark "end of A.ctor func"
        |ret
        |meta translator_mark "B.ctor func"
        |@func_B.ctor:
        |meta translator_mark "B.ctor func prefix"
        |new {}
        |dup
        |push @B.AnswerB
        |struct_mut "AnswerB"
        |push int32(2)
        |swapn
        |meta translator_mark "B.ctor local vars definition"
        |meta translator_mark "B.ctor func body"
        |push int32(2)
        |dupn
        |pop
        |push int32(2)
        |dupn
        |push int32(2)
        |dupn
        |struct_mut "BVal"
        |meta translator_mark "B.ctor local vars clearing"
        |pop
        |meta translator_mark "end of B.ctor func"
        |ret
        |meta translator_mark "helper functions"
        |@stop:
      """.stripMargin).right.get
      )
    }
  }
}
