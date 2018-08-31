package pravda.dotnet

package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object SmartProgramTests extends TestSuite {

  val tests = Tests {
    'smartProgramTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("smart_program.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler
          .parse("""
           |meta translator_mark "jump to methods"
           |dup
           |push "balanceOf"
           |eq
           |jumpi @method_balanceOf
           |dup
           |push "transfer"
           |eq
           |jumpi @method_transfer
           |push "Wrong method name"
           |throw
           |meta translator_mark "balanceOf method"
           |meta method {
           |"name":"balanceOf",int32(0):int8(14),"returnTpe":int8(3)
           |}
           |@method_balanceOf:
           |meta translator_mark "balanceOf local vars definition"
           |push null
           |meta translator_mark "balanceOf method body"
           |push x62616C616E636573
           |push int32(4)
           |dupn
           |push int32(0)
           |call @storage_get_default
           |push int32(2)
           |swapn
           |pop
           |push int32(1)
           |dupn
           |meta translator_mark "balanceOf local vars clearing"
           |swap
           |pop
           |swap
           |pop
           |swap
           |pop
           |meta translator_mark "end of balanceOf method"
           |jump @stop
           |meta translator_mark "ctor method"
           |@method_ctor:
           |meta translator_mark "ctor local vars definition"
           |meta translator_mark "ctor method body"
           |meta translator_mark "ctor local vars clearing"
           |meta translator_mark "end of ctor method"
           |ret
           |meta translator_mark "transfer method"
           |meta method {
           |"name":"transfer",int32(1):int8(3),int32(0):int8(14),"returnTpe":int8(0)
           |}
           |@method_transfer:
           |meta translator_mark "transfer local vars definition"
           |push null
           |push null
           |meta translator_mark "transfer method body"
           |push int32(4)
           |dupn
           |push int32(0)
           |swap
           |gt
           |push int8(3)
           |cast
           |push int32(3)
           |swapn
           |pop
           |push int32(2)
           |dupn
           |push int8(9)
           |cast
           |not
           |push int8(3)
           |cast
           |push int32(1)
           |eq
           |jumpi @transfer_br104
           |push x62616C616E636573
           |from
           |push int32(0)
           |call @storage_get_default
           |push int32(5)
           |dupn
           |swap
           |lt
           |push int8(3)
           |cast
           |push int32(0)
           |eq
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
           |jumpi @transfer_br103
           |push x62616C616E636573
           |from
           |push x62616C616E636573
           |from
           |push int32(0)
           |call @storage_get_default
           |push int32(7)
           |dupn
           |push int32(-1)
           |mul
           |add
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
           |push x62616C616E636573
           |push int32(6)
           |dupn
           |push x62616C616E636573
           |push int32(8)
           |dupn
           |push int32(0)
           |call @storage_get_default
           |push int32(7)
           |dupn
           |add
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
           |@transfer_br103:
           |@transfer_br104:
           |meta translator_mark "transfer local vars clearing"
           |pop
           |pop
           |pop
           |pop
           |pop
           |meta translator_mark "end of transfer method"
           |jump @stop
           |meta translator_mark "helper functions"
           |@storage_get_default:
           |push int32(2)
           |dupn
           |push int8(14)
           |cast
           |push int32(4)
           |dupn
           |concat
           |sexist
           |jumpi @get_default_if
           |swap
           |pop
           |swap
           |pop
           |ret
           |@get_default_if:
           |pop
           |push int8(14)
           |cast
           |swap
           |concat
           |sget
           |ret
           |@stop:
          """.stripMargin)
          .right
          .get
      )
    }

    'smartProgramTranslationWithPdb - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("smart_program.exe")
      val Right((_, pdbTables)) = parsePdbFile("smart_program.pdb")
      val src = "/tmp/pravda/smart_program.cs"

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures, Some(pdbTables)).right.get,
        PravdaAssembler.parse(s"""
             |meta translator_mark "jump to methods"
             |dup
             |push "balanceOf"
             |eq
             |jumpi @method_balanceOf
             |dup
             |push "transfer"
             |eq
             |jumpi @method_transfer
             |push "Wrong method name"
             |throw
             |meta translator_mark "balanceOf method"
             |meta method {
             |"name":"balanceOf",int32(0):int8(14),"returnTpe":int8(3)
             |}
             |@method_balanceOf:
             |meta translator_mark "balanceOf local vars definition"
             |push null
             |meta translator_mark "balanceOf method body"
             |meta source_mark {
             |"sl":int32(8),"sc":int32(44),"el":int32(8),"src":"$src","ec":int32(45)
             |}
             |meta source_mark {
             |"sl":int32(9),"sc":int32(9),"el":int32(9),"src":"$src","ec":int32(51)
             |}
             |push x62616C616E636573
             |push int32(4)
             |dupn
             |push int32(0)
             |call @storage_get_default
             |push int32(2)
             |swapn
             |pop
             |push int32(1)
             |dupn
             |meta source_mark {
             |"sl":int32(10),"sc":int32(5),"el":int32(10),"src":"$src","ec":int32(6)
             |}
             |meta translator_mark "balanceOf local vars clearing"
             |swap
             |pop
             |swap
             |pop
             |swap
             |pop
             |meta translator_mark "end of balanceOf method"
             |jump @stop
             |meta translator_mark "ctor method"
             |@method_ctor:
             |meta translator_mark "ctor local vars definition"
             |meta translator_mark "ctor method body"
             |meta source_mark {
             |"sl":int32(23),"sc":int32(31),"el":int32(23),"src":"$src","ec":int32(32)
             |}
             |meta source_mark {
             |"sl":int32(23),"sc":int32(32),"el":int32(23),"src":"$src","ec":int32(33)
             |}
             |meta translator_mark "ctor local vars clearing"
             |meta translator_mark "end of ctor method"
             |ret
             |meta translator_mark "transfer method"
             |meta method {
             |"name":"transfer",int32(1):int8(3),int32(0):int8(14),"returnTpe":int8(0)
             |}
             |@method_transfer:
             |meta translator_mark "transfer local vars definition"
             |push null
             |push null
             |meta translator_mark "transfer method body"
             |meta source_mark {
             |"sl":int32(12),"sc":int32(48),"el":int32(12),"src":"$src","ec":int32(49)
             |}
             |meta source_mark {
             |"sl":int32(13),"sc":int32(9),"el":int32(13),"src":"$src","ec":int32(24)
             |}
             |push int32(4)
             |dupn
             |push int32(0)
             |swap
             |gt
             |push int8(3)
             |cast
             |push int32(3)
             |swapn
             |pop
             |push int32(2)
             |dupn
             |push int8(9)
             |cast
             |not
             |push int8(3)
             |cast
             |push int32(1)
             |eq
             |jumpi @transfer_br104
             |meta source_mark {
             |"sl":int32(13),"sc":int32(25),"el":int32(13),"src":"$src","ec":int32(26)
             |}
             |meta source_mark {
             |"sl":int32(14),"sc":int32(13),"el":int32(14),"src":"$src","ec":int32(65)
             |}
             |push x62616C616E636573
             |from
             |push int32(0)
             |call @storage_get_default
             |push int32(5)
             |dupn
             |swap
             |lt
             |push int8(3)
             |cast
             |push int32(0)
             |eq
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
             |jumpi @transfer_br103
             |meta source_mark {
             |"sl":int32(14),"sc":int32(66),"el":int32(14),"src":"$src","ec":int32(67)
             |}
             |meta source_mark {
             |"sl":int32(15),"sc":int32(17),"el":int32(15),"src":"$src","ec":int32(93)
             |}
             |push x62616C616E636573
             |from
             |push x62616C616E636573
             |from
             |push int32(0)
             |call @storage_get_default
             |push int32(7)
             |dupn
             |push int32(-1)
             |mul
             |add
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
             |meta source_mark {
             |"sl":int32(16),"sc":int32(17),"el":int32(16),"src":"$src","ec":int32(71)
             |}
             |push x62616C616E636573
             |push int32(6)
             |dupn
             |push x62616C616E636573
             |push int32(8)
             |dupn
             |push int32(0)
             |call @storage_get_default
             |push int32(7)
             |dupn
             |add
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
             |@transfer_br103:
             |@transfer_br104:
             |meta source_mark {
             |"sl":int32(17),"sc":int32(13),"el":int32(17),"src":"$src","ec":int32(14)
             |}
             |meta translator_mark "transfer local vars clearing"
             |pop
             |pop
             |pop
             |pop
             |pop
             |meta translator_mark "end of transfer method"
             |jump @stop
             |meta translator_mark "helper functions"
             |@storage_get_default:
             |push int32(2)
             |dupn
             |push int8(14)
             |cast
             |push int32(4)
             |dupn
             |concat
             |sexist
             |jumpi @get_default_if
             |swap
             |pop
             |swap
             |pop
             |ret
             |@get_default_if:
             |pop
             |push int8(14)
             |cast
             |swap
             |concat
             |sget
             |ret
             |@stop:
          """.stripMargin).right.get
      )
    }
  }
}
