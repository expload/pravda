package pravda.dotnet
package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object SystemTests extends TestSuite {

  val tests = Tests {
    'systemTranslation - {
      val Right((_, cilData, methods, signatures)) = parseFile("system.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse(
          """
            |push null
            |sexist
            |jumpi @methods
            |call @ctor
            |@methods:
            |meta method {
            |int8(-1):"system",int8(-2):int8(0)
            |}
            |dup
            |push "system"
            |eq
            |jumpi @method_system
            |jump @stop
            |@method_system:
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push x
            |owner
            |push int32(5)
            |swapn
            |pop
            |push x
            |balance
            |push int32(4)
            |swapn
            |pop
            |push x0000000000000000000000000000000000000000000000000000000000000000
            |push int32(3)
            |swapn
            |pop
            |paddr
            |push int32(2)
            |swapn
            |pop
            |pop
            |pop
            |pop
            |pop
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
