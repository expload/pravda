package pravda.dotnet
package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object StaticClassTests extends TestSuite {

  val tests = Tests {
    'staticClassTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("static_class.exe")

      //println(PravdaAssembler.render(Translator.translateAsm(methods, cilData, signatures).right.get))

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
            |push "ToHex"
            |eq
            |jumpi @method_ToHex
            |push "Wrong method name"
            |throw
            |meta translator_mark "ToHex method"
            |meta method {
            |"name":"ToHex",int32(0):int8(14),"returnTpe":int8(11)
            |}
            |@method_ToHex:
            |meta translator_mark "ToHex local vars definition"
            |meta translator_mark "ToHex method body"
            |push int32(2)
            |dupn
            |call @func_Com.Expload.StringUtils.BytesToHex_Com.Expload.Bytes
            |jump @stop
            |meta translator_mark "ToHex local vars clearing"
            |swap
            |pop
            |swap
            |pop
            |meta translator_mark "end of ToHex method"
            |jump @stop
            |meta translator_mark "ctor method"
            |meta method {
            |"name":"ctor","returnTpe":int8(0)
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
            |jump @stop
            |meta translator_mark "ctor local vars clearing"
            |pop
            |meta translator_mark "end of ctor method"
            |jump @stop
            |meta translator_mark "Com.Expload.StringUtils.ByteToHex_uint8 func"
            |@func_Com.Expload.StringUtils.ByteToHex_uint8:
            |meta translator_mark "Com.Expload.StringUtils.ByteToHex_uint8 local vars definition"
            |meta translator_mark "Com.Expload.StringUtils.ByteToHex_uint8 func body"
            |push int32(2)
            |dupn
            |push int32(16)
            |swap
            |div
            |call @func_Com.Expload.StringUtils.HexPart_int32
            |push int32(3)
            |dupn
            |push int32(16)
            |swap
            |mod
            |call @func_Com.Expload.StringUtils.HexPart_int32
            |swap
            |concat
            |ret
            |meta translator_mark "Com.Expload.StringUtils.ByteToHex_uint8 local vars clearing"
            |swap
            |pop
            |meta translator_mark "end of Com.Expload.StringUtils.ByteToHex_uint8 func"
            |ret
            |meta translator_mark "Com.Expload.StringUtils.BytesToHex_Com.Expload.Bytes func"
            |@func_Com.Expload.StringUtils.BytesToHex_Com.Expload.Bytes:
            |meta translator_mark "Com.Expload.StringUtils.BytesToHex_Com.Expload.Bytes local vars definition"
            |push null
            |push null
            |meta translator_mark "Com.Expload.StringUtils.BytesToHex_Com.Expload.Bytes func body"
            |push ""
            |push int32(3)
            |swapn
            |pop
            |push int32(0)
            |push int32(2)
            |swapn
            |pop
            |jump @Com.Expload.StringUtils.BytesToHex_Com.Expload.Bytes_br33
            |@Com.Expload.StringUtils.BytesToHex_Com.Expload.Bytes_br10:
            |push int32(2)
            |dupn
            |push int32(5)
            |dupn
            |push int32(3)
            |dupn
            |array_get
            |call @func_Com.Expload.StringUtils.ByteToHex_uint8
            |swap
            |concat
            |push int32(3)
            |swapn
            |pop
            |push int32(1)
            |dupn
            |push int32(1)
            |add
            |push int32(2)
            |swapn
            |pop
            |@Com.Expload.StringUtils.BytesToHex_Com.Expload.Bytes_br33:
            |push int32(1)
            |dupn
            |push int32(5)
            |dupn
            |length
            |swap
            |lt
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.BytesToHex_Com.Expload.Bytes_br10
            |push int32(2)
            |dupn
            |ret
            |meta translator_mark "Com.Expload.StringUtils.BytesToHex_Com.Expload.Bytes local vars clearing"
            |swap
            |pop
            |swap
            |pop
            |swap
            |pop
            |meta translator_mark "end of Com.Expload.StringUtils.BytesToHex_Com.Expload.Bytes func"
            |ret
            |meta translator_mark "Com.Expload.StringUtils.HexPart_int32 func"
            |@func_Com.Expload.StringUtils.HexPart_int32:
            |meta translator_mark "Com.Expload.StringUtils.HexPart_int32 local vars definition"
            |meta translator_mark "Com.Expload.StringUtils.HexPart_int32 func body"
            |push int32(2)
            |dupn
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br9
            |push "0"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br9:
            |push int32(2)
            |dupn
            |push int32(1)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br19
            |push "1"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br19:
            |push int32(2)
            |dupn
            |push int32(2)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br29
            |push "2"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br29:
            |push int32(2)
            |dupn
            |push int32(3)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br39
            |push "3"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br39:
            |push int32(2)
            |dupn
            |push int32(4)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br49
            |push "4"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br49:
            |push int32(2)
            |dupn
            |push int32(5)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br59
            |push "5"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br59:
            |push int32(2)
            |dupn
            |push int32(6)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br69
            |push "6"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br69:
            |push int32(2)
            |dupn
            |push int32(7)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br79
            |push "7"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br79:
            |push int32(2)
            |dupn
            |push int32(8)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br89
            |push "8"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br89:
            |push int32(2)
            |dupn
            |push int32(9)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br100
            |push "9"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br100:
            |push int32(2)
            |dupn
            |push int32(10)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br111
            |push "A"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br111:
            |push int32(2)
            |dupn
            |push int32(11)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br122
            |push "B"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br122:
            |push int32(2)
            |dupn
            |push int32(12)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br133
            |push "C"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br133:
            |push int32(2)
            |dupn
            |push int32(13)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br144
            |push "D"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br144:
            |push int32(2)
            |dupn
            |push int32(14)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br155
            |push "E"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br155:
            |push int32(2)
            |dupn
            |push int32(15)
            |eq
            |push int8(3)
            |cast
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @Com.Expload.StringUtils.HexPart_int32_br166
            |push "F"
            |ret
            |@Com.Expload.StringUtils.HexPart_int32_br166:
            |push ""
            |ret
            |meta translator_mark "Com.Expload.StringUtils.HexPart_int32 local vars clearing"
            |swap
            |pop
            |meta translator_mark "end of Com.Expload.StringUtils.HexPart_int32 func"
            |ret
            |meta translator_mark "helper functions"
            |@stop:
          """.stripMargin).right.get
      )
    }
  }
}
