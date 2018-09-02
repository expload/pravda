package pravda.dotnet

package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object StdLibTests extends TestSuite {

  val tests = Tests {
    'stdLibTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("stdlib.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler
          .parse(
            """
            |meta translator_mark "jump to methods"
            |dup
            |push "Ripemd160"
            |eq
            |jumpi @method_Ripemd160
            |dup
            |push "ValidateEd25519Signature"
            |eq
            |jumpi @method_ValidateEd25519Signature
            |push "Wrong method name"
            |throw
            |meta translator_mark "Ripemd160 method"
            |meta method {
            |"name":"Ripemd160",int32(0):int8(11),"returnTpe":int8(14)
            |}
            |@method_Ripemd160:
            |meta translator_mark "Ripemd160 local vars definition"
            |push null
            |meta translator_mark "Ripemd160 method body"
            |push int32(3)
            |dupn
            |push int32(2)
            |scall
            |push int32(2)
            |swapn
            |pop
            |push int32(1)
            |dupn
            |meta translator_mark "Ripemd160 local vars clearing"
            |swap
            |pop
            |swap
            |pop
            |swap
            |pop
            |meta translator_mark "end of Ripemd160 method"
            |jump @stop
            |meta translator_mark "ValidateEd25519Signature method"
            |meta method {
            |"name":"ValidateEd25519Signature",int32(1):int8(11),int32(2):int8(14),int32(0):int8(14),"returnTpe":int8(9)
            |}
            |@method_ValidateEd25519Signature:
            |meta translator_mark "ValidateEd25519Signature local vars definition"
            |push null
            |meta translator_mark "ValidateEd25519Signature method body"
            |push int32(5)
            |dupn
            |push int32(5)
            |dupn
            |push int32(5)
            |dupn
            |push int32(1)
            |scall
            |push int32(2)
            |swapn
            |pop
            |push int32(1)
            |dupn
            |meta translator_mark "ValidateEd25519Signature local vars clearing"
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
            |meta translator_mark "end of ValidateEd25519Signature method"
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
          """.stripMargin)
          .right
          .get
      )
    }
  }
}
