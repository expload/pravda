package pravda.dotnet
package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object StdLibTests extends TestSuite {

  val tests = Tests {
    'stdLibTranslation - {
      val Right((_, cilData, methods, signatures)) = parseFile("stdlib.exe")

      println(PravdaAssembler.render(Translator.translateAsm(methods, cilData, signatures).right.get))

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse("""
            |push null
            |sexist
            |jumpi @methods
            |call @ctor
            |@methods:
            |meta method {
            |int8(-1):"Ripemd160",int8(-2):int8(14),int8(0):int8(14)
            |}
            |meta method {
            |int8(4):int8(14),int8(-1):"ValidateEd25519Signature",int8(-2):int8(9),int8(0):int8(14),int8(2):int8(14)
            |}
            |dup
            |push "Ripemd160"
            |eq
            |jumpi @method_Ripemd160
            |dup
            |push "ValidateEd25519Signature"
            |eq
            |jumpi @method_ValidateEd25519Signature
            |jump @stop
            |@method_Ripemd160:
            |push int32(0)
            |push int32(3)
            |dupn
            |push int32(2)
            |scall
            |push int32(2)
            |swapn
            |pop
            |push int32(1)
            |dupn
            |swap
            |pop
            |swap
            |pop
            |swap
            |pop
            |jump @stop
            |@method_ValidateEd25519Signature:
            |push int32(0)
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
            |jump @stop
            |@ctor:
            |push null
            |dup
            |sput
            |ret
            |@stop:
          """.stripMargin).right.get
      )
    }
  }
}
