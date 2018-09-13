package pravda.dotnet

package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object InheritanceTests extends TestSuite {

  val tests = Tests {
    'inheritanceTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("inheritance.exe")

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
           |push "Func"
           |eq
           |jumpi @method_Func
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
           |push null
           |push null
           |meta translator_mark "Func method body"
           |push int32(100)
           |new {}
           |call @vtable_A
           |push int32(2)
           |swapn
           |call @func_A.ctor_int32
           |push int32(7)
           |swapn
           |pop
           |push int32(200)
           |new {}
           |call @vtable_B
           |push int32(2)
           |swapn
           |call @func_B.ctor_int32
           |push int32(6)
           |swapn
           |pop
           |push int32(6)
           |dupn
           |push int32(1)
           |dupn
           |struct_get "Answer"
           |call
           |swap
           |pop
           |push int32(6)
           |dupn
           |push int32(1)
           |dupn
           |struct_get "Answer"
           |call
           |swap
           |pop
           |add
           |push int32(5)
           |swapn
           |pop
           |push int32(6)
           |dupn
           |push int32(1)
           |dupn
           |struct_get "AnswerPlus1"
           |call
           |swap
           |pop
           |push int32(4)
           |swapn
           |pop
           |push int32(5)
           |dupn
           |push int32(1)
           |dupn
           |struct_get "AnswerPlus1"
           |call
           |swap
           |pop
           |push int32(3)
           |swapn
           |pop
           |push int32(3)
           |dupn
           |push int32(3)
           |dupn
           |add
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
           |swap
           |pop
           |swap
           |pop
           |meta translator_mark "end of Func method"
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
           |meta translator_mark "end of A.Answer func"
           |ret
           |meta translator_mark "A.ctor_int32 func"
           |@func_A.ctor_int32:
           |meta translator_mark "A.ctor_int32 local vars definition"
           |meta translator_mark "A.ctor_int32 func body"
           |push int32(2)
           |dupn
           |push int32(2)
           |dupn
           |call @func_Parent.ctor_int32
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
           |meta translator_mark "end of B.Answer func"
           |ret
           |meta translator_mark "B.ctor_int32 func"
           |@func_B.ctor_int32:
           |meta translator_mark "B.ctor_int32 local vars definition"
           |meta translator_mark "B.ctor_int32 func body"
           |push int32(2)
           |dupn
           |push int32(2)
           |dupn
           |call @func_Parent.ctor_int32
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
           |meta translator_mark "end of Parent.Answer func"
           |ret
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
           |swap
           |pop
           |push int32(1)
           |add
           |push int32(2)
           |swapn
           |pop
           |push int32(1)
           |dupn
           |meta translator_mark "Parent.AnswerPlus1 local vars clearing"
           |swap
           |pop
           |meta translator_mark "end of Parent.AnswerPlus1 func"
           |ret
           |meta translator_mark "Parent.ctor_int32 func"
           |@func_Parent.ctor_int32:
           |meta translator_mark "Parent.ctor_int32 local vars definition"
           |meta translator_mark "Parent.ctor_int32 func body"
           |push int32(2)
           |dupn
           |pop
           |meta translator_mark "Parent.ctor_int32 local vars clearing"
           |pop
           |meta translator_mark "end of Parent.ctor_int32 func"
           |ret
           |meta translator_mark "A vtable initialization"
           |@vtable_A:
           |dup
           |push @func_A.Answer
           |struct_mut "Answer"
           |dup
           |push @func_Parent.AnswerPlus1
           |struct_mut "AnswerPlus1"
           |ret
           |meta translator_mark "B vtable initialization"
           |@vtable_B:
           |dup
           |push @func_B.Answer
           |struct_mut "Answer"
           |dup
           |push @func_Parent.AnswerPlus1
           |struct_mut "AnswerPlus1"
           |ret
           |meta translator_mark "Parent vtable initialization"
           |@vtable_Parent:
           |dup
           |push @func_Parent.AnswerPlus1
           |struct_mut "AnswerPlus1"
           |dup
           |push @func_Parent.Answer
           |struct_mut "Answer"
           |ret
           |meta translator_mark "helper functions"
           |@stop:
           |
      """.stripMargin).right.get
      )
    }
  }
}
