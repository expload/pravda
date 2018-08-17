package pravda.dotnet
package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object InheritanceTests extends TestSuite {

  val tests = Tests {
    'objectsTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("inheritance.exe")

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
           |push null
           |push null
           |meta translator_mark "Func method body"
           |push int32(100)
           |call @func_A.ctor
           |push int32(5)
           |swapn
           |pop
           |push int32(200)
           |call @func_B.ctor
           |push int32(3)
           |swapn
           |pop
           |push int32(3)
           |dupn
           |push int32(1)
           |dupn
           |struct_get "Answer"
           |call
           |push int32(2)
           |dupn
           |push int32(1)
           |dupn
           |struct_get "Answer"
           |call
           |add
           |push int32(0)
           |swapn
           |pop
           |push int32(1)
           |dupn
           |push int32(1)
           |dupn
           |struct_get "AnswerPlus1"
           |call
           |push int32(-2)
           |swapn
           |pop
           |push int32(-1)
           |dupn
           |push int32(1)
           |dupn
           |struct_get "AnswerPlus1"
           |call
           |push int32(-4)
           |swapn
           |pop
           |meta translator_mark "Func local vars clearing"
           |pop
           |pop
           |pop
           |pop
           |pop
           |pop
           |meta translator_mark "end of Func method"
           |jump @stop
           |meta translator_mark "Parent.AnswerPlus1 func"
           |@func_Parent.AnswerPlus1:
           |meta translator_mark "Parent.AnswerPlus1 local vars definition"
           |push null
           |meta translator_mark "Parent.AnswerPlus1 func body"
           |push int32(2)
           |dupn
           |push int32(1)
           |dupn
           |struct_get "Answer"
           |call
           |push int32(1)
           |add
           |push int32(1)
           |swapn
           |pop
           |push int32(0)
           |dupn
           |meta translator_mark "Parent.AnswerPlus1 local vars clearing"
           |swap
           |pop
           |swap
           |pop
           |meta translator_mark "end of Parent.AnswerPlus1 func"
           |ret
           |meta translator_mark "Parent.Answer func"
           |@func_Parent.Answer:
           |meta translator_mark "Parent.Answer local vars definition"
           |push null
           |meta translator_mark "Parent.Answer func body"
           |push int32(0)
           |push int32(2)
           |swapn
           |pop
           |push int32(1)
           |dupn
           |meta translator_mark "Parent.Answer local vars clearing"
           |swap
           |pop
           |swap
           |pop
           |meta translator_mark "end of Parent.Answer func"
           |ret
           |meta translator_mark "A.Answer func"
           |@func_A.Answer:
           |meta translator_mark "A.Answer local vars definition"
           |push null
           |meta translator_mark "A.Answer func body"
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
           |meta translator_mark "A.Answer local vars clearing"
           |swap
           |pop
           |swap
           |pop
           |meta translator_mark "end of A.Answer func"
           |ret
           |meta translator_mark "B.Answer func"
           |@func_B.Answer:
           |meta translator_mark "B.Answer local vars definition"
           |push null
           |meta translator_mark "B.Answer func body"
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
           |meta translator_mark "B.Answer local vars clearing"
           |swap
           |pop
           |swap
           |pop
           |meta translator_mark "end of B.Answer func"
           |ret
           |meta translator_mark "Parent.ctor func"
           |@func_Parent.ctor:
           |meta translator_mark "Parent.ctor func prefix"
           |new {}
           |dup
           |push @Parent.AnswerPlus1
           |struct_mut "AnswerPlus1"
           |dup
           |push @Parent.Answer
           |struct_mut "Answer"
           |push int32(2)
           |swapn
           |meta translator_mark "Parent.ctor local vars definition"
           |meta translator_mark "Parent.ctor func body"
           |push int32(2)
           |dupn
           |pop
           |meta translator_mark "Parent.ctor local vars clearing"
           |pop
           |meta translator_mark "end of Parent.ctor func"
           |ret
           |meta translator_mark "A.ctor func"
           |@func_A.ctor:
           |meta translator_mark "A.ctor func prefix"
           |new {}
           |dup
           |push @A.Answer
           |struct_mut "Answer"
           |dup
           |push @Parent.AnswerPlus1
           |struct_mut "AnswerPlus1"
           |push int32(2)
           |swapn
           |meta translator_mark "A.ctor local vars definition"
           |meta translator_mark "A.ctor func body"
           |push int32(2)
           |dupn
           |push int32(2)
           |dupn
           |push int32(2)
           |dupn
           |struct_get ".ctor_int32"
           |call
           |push int32(3)
           |dupn
           |push int32(3)
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
           |push @B.Answer
           |struct_mut "Answer"
           |dup
           |push @Parent.AnswerPlus1
           |struct_mut "AnswerPlus1"
           |push int32(2)
           |swapn
           |meta translator_mark "B.ctor local vars definition"
           |meta translator_mark "B.ctor func body"
           |push int32(2)
           |dupn
           |push int32(2)
           |dupn
           |push int32(2)
           |dupn
           |struct_get ".ctor_int32"
           |call
           |push int32(3)
           |dupn
           |push int32(3)
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
