package pravda.dotnet
package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object StringTests extends TestSuite {

  val tests = Tests {
    'stringTranslation - {
      val Right((_, cilData, methods, signatures)) = parseFile("strings.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse("""
            |push null
            |sexist
            |jumpi @methods
            |call @ctor
            |@methods:
            |meta method { int8(-1): "distributeSalary", int8(-2): int8(0) }
            |dup
            |push "distributeSalary"
            |eq
            |jumpi @method_distributeSalary
            |jump @stop
            |@method_distributeSalary:
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push "zapupu"
            |push int32(9)
            |swapn
            |pop
            |push "lu"
            |push int32(8)
            |swapn
            |pop
            |push "pa"
            |push int32(7)
            |swapn
            |pop
            |push int32(7)
            |dupn
            |push int32(7)
            |dupn
            |swap
            |concat
            |push int32(6)
            |swapn
            |pop
            |push x737472696E6773
            |push int32(6)
            |dupn
            |push int32(10)
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
            |push x737472696E6773
            |push "lupa"
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
            |jumpi @distributeSalary_br87
            |push x737472696E6773
            |push "pupa"
            |push ""
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
            |@distributeSalary_br87:
            |push int32(8)
            |dupn
            |push int32(0)
            |array_get
            |push int32(5)
            |swapn
            |pop
            |push int32(5)
            |dupn
            |push int32(3)
            |array_get
            |push int32(4)
            |swapn
            |pop
            |push int32(5)
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
            |pop
            |pop
            |pop
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
