package pravda.dotnet

package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object StringTests extends TestSuite {

  val tests = Tests {
    'stringTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("strings.exe")

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
            |push "ctor"
            |eq
            |jumpi @method_ctor
            |dup
            |push "distributeSalary"
            |eq
            |jumpi @method_distributeSalary
            |push "Wrong method name"
            |throw
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
            |meta translator_mark "distributeSalary method"
            |meta method {
            |"name":"distributeSalary","returnTpe":int8(0)
            |}
            |@method_distributeSalary:
            |meta translator_mark "distributeSalary local vars definition"
            |push null
            |push null
            |push null
            |push null
            |push null
            |push null
            |push null
            |push null
            |meta translator_mark "distributeSalary method body"
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
            |meta translator_mark "distributeSalary local vars clearing"
            |pop
            |pop
            |pop
            |pop
            |pop
            |pop
            |pop
            |pop
            |pop
            |meta translator_mark "end of distributeSalary method"
            |jump @stop
            |meta translator_mark "helper functions"
            |@stop:
          """.stripMargin).right.get
      )
    }
  }
}
