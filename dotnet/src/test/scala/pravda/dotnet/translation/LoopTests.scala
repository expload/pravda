package pravda.dotnet

package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object LoopTests extends TestSuite {

  val tests = Tests {
    'loopTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("loop.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse("""
            |meta translator_mark "jump to methods"
            |meta method {
            |"name":"loops","returnTpe":int8(0)
            |}
            |dup
            |push "loops"
            |eq
            |jumpi @method_loops
            |push "Wrong method name"
            |throw
            |meta translator_mark "loops method"
            |@method_loops:
            |meta translator_mark "loops local vars definition"
            |push null
            |push null
            |push null
            |push null
            |meta translator_mark "loops method body"
            |push int32(0)
            |push int32(5)
            |swapn
            |pop
            |push int32(0)
            |push int32(4)
            |swapn
            |pop
            |jump @loops_br17
            |@loops_br7:
            |push int32(4)
            |dupn
            |push int32(2)
            |add
            |push int32(5)
            |swapn
            |pop
            |push int32(3)
            |dupn
            |push int32(1)
            |add
            |push int32(4)
            |swapn
            |pop
            |@loops_br17:
            |push int32(3)
            |dupn
            |push int32(10)
            |swap
            |lt
            |push int8(3)
            |cast
            |push int32(3)
            |swapn
            |pop
            |push int32(2)
            |dupn
            |push int32(1)
            |eq
            |jumpi @loops_br7
            |jump @loops_br34
            |@loops_br28:
            |push int32(4)
            |dupn
            |push int32(2)
            |mul
            |push int32(5)
            |swapn
            |pop
            |@loops_br34:
            |push int32(4)
            |dupn
            |push int32(10000)
            |swap
            |lt
            |push int8(3)
            |cast
            |push int32(2)
            |swapn
            |pop
            |push int32(1)
            |dupn
            |push int32(1)
            |eq
            |jumpi @loops_br28
            |meta translator_mark "loops local vars clearing"
            |pop
            |pop
            |pop
            |pop
            |pop
            |meta translator_mark "end of loops method"
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

    'nestedLoopTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("loop_nested.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse("""
            |meta translator_mark "jump to methods"
            |meta method {
            |"name":"loops","returnTpe":int8(0)
            |}
            |dup
            |push "loops"
            |eq
            |jumpi @method_loops
            |push "Wrong method name"
            |throw
            |meta translator_mark "loops method"
            |@method_loops:
            |meta translator_mark "loops local vars definition"
            |push null
            |push null
            |push null
            |push null
            |push null
            |push null
            |push null
            |meta translator_mark "loops method body"
            |push int32(0)
            |push int32(8)
            |swapn
            |pop
            |push int32(0)
            |push int32(7)
            |swapn
            |pop
            |jump @loops_br58
            |@loops_br7:
            |push int32(0)
            |push int32(6)
            |swapn
            |pop
            |jump @loops_br42
            |@loops_br12:
            |push int32(7)
            |dupn
            |push int32(2)
            |swap
            |mod
            |push int32(0)
            |eq
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
            |jumpi @loops_br37
            |push int32(7)
            |dupn
            |push int32(7)
            |dupn
            |push int32(7)
            |dupn
            |add
            |push int32(1000000007)
            |swap
            |mod
            |add
            |push int32(8)
            |swapn
            |pop
            |@loops_br37:
            |push int32(5)
            |dupn
            |push int32(1)
            |add
            |push int32(6)
            |swapn
            |pop
            |@loops_br42:
            |push int32(5)
            |dupn
            |push int32(20)
            |swap
            |lt
            |push int8(3)
            |cast
            |push int32(4)
            |swapn
            |pop
            |push int32(3)
            |dupn
            |push int32(1)
            |eq
            |jumpi @loops_br12
            |push int32(6)
            |dupn
            |push int32(1)
            |add
            |push int32(7)
            |swapn
            |pop
            |@loops_br58:
            |push int32(6)
            |dupn
            |push int32(10)
            |swap
            |lt
            |push int8(3)
            |cast
            |push int32(3)
            |swapn
            |pop
            |push int32(2)
            |dupn
            |push int32(1)
            |eq
            |jumpi @loops_br7
            |jump @loops_br77
            |@loops_br71:
            |push int32(7)
            |dupn
            |push int32(2)
            |mul
            |push int32(8)
            |swapn
            |pop
            |@loops_br77:
            |push int32(7)
            |dupn
            |push int32(10000)
            |swap
            |lt
            |push int8(3)
            |cast
            |push int32(2)
            |swapn
            |pop
            |push int32(1)
            |dupn
            |push int32(1)
            |eq
            |jumpi @loops_br71
            |meta translator_mark "loops local vars clearing"
            |pop
            |pop
            |pop
            |pop
            |pop
            |pop
            |pop
            |pop
            |meta translator_mark "end of loops method"
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
