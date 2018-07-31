package pravda.dotnet
package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object ArraysTests extends TestSuite {

  val tests = Tests {
    'arrayTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("arrays.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse("""
        |meta translator_mark "jump to methods"
        |meta method {
        |"name":"WorkWithBytes","returnTpe":int8(0)
        |}
        |meta method {
        |"name":"WorkWithArrays","returnTpe":int8(0)
        |}
        |dup
        |push "WorkWithBytes"
        |eq
        |jumpi @method_WorkWithBytes
        |dup
        |push "WorkWithArrays"
        |eq
        |jumpi @method_WorkWithArrays
        |jump @stop
        |meta translator_mark "WorkWithBytes method"
        |@method_WorkWithBytes:
        |meta translator_mark "WorkWithBytes local vars definition"
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |meta translator_mark "WorkWithBytes method body"
        |new int8[1, 2, 3]
        |push int32(10)
        |swapn
        |pop
        |new int8[4, 5, 6]
        |call @array_to_bytes
        |push int32(9)
        |swapn
        |pop
        |new int8[7, 8, 9]
        |call @array_to_bytes
        |push int32(8)
        |swapn
        |pop
        |push int32(9)
        |dupn
        |push int32(0)
        |array_get
        |push int32(7)
        |swapn
        |pop
        |push int32(9)
        |dupn
        |push int32(2)
        |array_get
        |push int32(6)
        |swapn
        |pop
        |push int32(8)
        |dupn
        |push int32(1)
        |array_get
        |push int32(5)
        |swapn
        |pop
        |push int32(7)
        |dupn
        |push int32(1)
        |array_get
        |push int32(4)
        |swapn
        |pop
        |push int32(8)
        |dupn
        |push int32(1)
        |push int32(2)
        |push int32(2)
        |dupn
        |add
        |swap
        |slice
        |push int32(3)
        |swapn
        |pop
        |push x6279746573
        |push int32(9)
        |dupn
        |push int32(9)
        |dupn
        |push int32(2)
        |dupn
        |push int8(14)
        |cast
        |push int32(4)
        |dupn
        |concat
        |swap
        |sput
        |pop
        |pop
        |push x6279746573
        |new int8[8, 9, 10]
        |call @array_to_bytes
        |push int8(14)
        |cast
        |swap
        |concat
        |sexist
        |push int8(3)
        |cast
        |push int32(2)
        |swapn
        |pop
        |push int32(1)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @WorkWithBytes_br192
        |push x6279746573
        |push int32(9)
        |dupn
        |new int8[7, 8, 9]
        |call @array_to_bytes
        |push int32(2)
        |dupn
        |push int8(14)
        |cast
        |push int32(4)
        |dupn
        |concat
        |swap
        |sput
        |pop
        |pop
        |@WorkWithBytes_br192:
        |push int32(9)
        |dupn
        |push int32(0)
        |push int32(2)
        |swap
        |array_mut
        |push int32(9)
        |dupn
        |push int32(1)
        |push int32(1)
        |swap
        |array_mut
        |meta translator_mark "WorkWithBytes local vars clearing"
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |meta translator_mark "end of WorkWithBytes method"
        |jump @stop
        |meta translator_mark "WorkWithArrays method"
        |@method_WorkWithArrays:
        |meta translator_mark "WorkWithArrays local vars definition"
        |push null
        |push null
        |push null
        |push null
        |push null
        |meta translator_mark "WorkWithArrays method body"
        |new int16[97, 98, 99]
        |push int32(6)
        |swapn
        |pop
        |new int32[1, 2, 3]
        |push int32(5)
        |swapn
        |pop
        |new number[1.0, 2.0, 3.0]
        |push int32(4)
        |swapn
        |pop
        |push int32(3)
        |push int8(11)
        |new_array
        |dup
        |push int32(0)
        |push "abc"
        |swap
        |array_mut
        |dup
        |push int32(1)
        |push "def"
        |swap
        |array_mut
        |dup
        |push int32(2)
        |push "rty"
        |swap
        |array_mut
        |push int32(3)
        |swapn
        |pop
        |new int32[4, 5, 6]
        |push int32(2)
        |swapn
        |pop
        |push int32(5)
        |dupn
        |push int32(1)
        |push int32(100)
        |swap
        |array_mut
        |push int32(4)
        |dupn
        |push int32(1)
        |push int32(4)
        |swap
        |array_mut
        |push int32(3)
        |dupn
        |push int32(1)
        |push number(4.0)
        |swap
        |array_mut
        |push int32(2)
        |dupn
        |push int32(1)
        |push "asdf"
        |swap
        |array_mut
        |push int32(1)
        |dupn
        |push int32(1)
        |push int32(7)
        |swap
        |array_mut
        |meta translator_mark "WorkWithArrays local vars clearing"
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |meta translator_mark "end of WorkWithArrays method"
        |jump @stop
        |meta translator_mark "ctor method"
        |@method_ctor:
        |meta translator_mark "ctor local vars definition"
        |meta translator_mark "ctor method body"
        |meta translator_mark "ctor local vars clearing"
        |meta translator_mark "end of ctor method"
        |ret
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
