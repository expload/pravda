package pravda.dotnet
package translation

import pravda.vm.asm.PravdaAssembler
import utest._


object CompareTests extends TestSuite {

  val tests = Tests {
    'CompareTranslation - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("compare.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse(
          """
        |meta translator_mark "jump to methods"
        |meta method {
        |"name":"compare","returnTpe":int8(0)
        |}
        |dup
        |push "compare"
        |eq
        |jumpi @method_compare
        |push "Wrong method name"
        |throw
        |meta translator_mark "compare method"
        |@method_compare:
        |meta translator_mark "compare local vars definition"
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |push null
        |meta translator_mark "compare method body"
        |push int32(1)
        |push int32(38)
        |swapn
        |pop
        |push int32(2)
        |push int32(37)
        |swapn
        |pop
        |push int32(3)
        |push int32(36)
        |swapn
        |pop
        |push int32(4)
        |push int32(35)
        |swapn
        |pop
        |push int32(5)
        |push int8(4)
        |cast
        |push int32(34)
        |swapn
        |pop
        |push int32(6)
        |push int8(4)
        |cast
        |push int32(33)
        |swapn
        |pop
        |push int32(0)
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int32(37)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int8(4)
        |cast
        |push int32(36)
        |dupn
        |push int8(4)
        |cast
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(35)
        |dupn
        |push int32(35)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(35)
        |dupn
        |push int8(4)
        |cast
        |push int32(34)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(33)
        |dupn
        |push int32(33)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int32(37)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(31)
        |swapn
        |pop
        |push int32(30)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br68
        |@compare_br68:
        |push int32(37)
        |dupn
        |push int8(4)
        |cast
        |push int32(36)
        |dupn
        |push int8(4)
        |cast
        |eq
        |push int8(3)
        |cast
        |push int32(30)
        |swapn
        |pop
        |push int32(29)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br82
        |@compare_br82:
        |push int32(35)
        |dupn
        |push int32(35)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(29)
        |swapn
        |pop
        |push int32(28)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br94
        |@compare_br94:
        |push int32(35)
        |dupn
        |push int8(4)
        |cast
        |push int32(34)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(28)
        |swapn
        |pop
        |push int32(27)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br108
        |@compare_br108:
        |push int32(33)
        |dupn
        |push int32(33)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(27)
        |swapn
        |pop
        |push int32(26)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br122
        |@compare_br122:
        |push int32(37)
        |dupn
        |push int32(37)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int8(4)
        |cast
        |push int32(36)
        |dupn
        |push int8(4)
        |cast
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(35)
        |dupn
        |push int32(35)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(35)
        |dupn
        |push int8(4)
        |cast
        |push int32(34)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(33)
        |dupn
        |push int32(33)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int32(37)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(26)
        |swapn
        |pop
        |push int32(25)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br188
        |@compare_br188:
        |push int32(37)
        |dupn
        |push int8(4)
        |cast
        |push int32(36)
        |dupn
        |push int8(4)
        |cast
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(25)
        |swapn
        |pop
        |push int32(24)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br205
        |@compare_br205:
        |push int32(35)
        |dupn
        |push int32(35)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(24)
        |swapn
        |pop
        |push int32(23)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br220
        |@compare_br220:
        |push int32(35)
        |dupn
        |push int8(4)
        |cast
        |push int32(34)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(23)
        |swapn
        |pop
        |push int32(22)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br237
        |@compare_br237:
        |push int32(33)
        |dupn
        |push int32(33)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(22)
        |swapn
        |pop
        |push int32(21)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br254
        |@compare_br254:
        |push int32(37)
        |dupn
        |push int32(37)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int8(4)
        |cast
        |push int32(36)
        |dupn
        |push int8(4)
        |cast
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(35)
        |dupn
        |push int32(35)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(35)
        |dupn
        |push int8(4)
        |cast
        |push int32(34)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(33)
        |dupn
        |push int32(33)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int32(37)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(21)
        |swapn
        |pop
        |push int32(20)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br320
        |@compare_br320:
        |push int32(37)
        |dupn
        |push int8(4)
        |cast
        |push int32(36)
        |dupn
        |push int8(4)
        |cast
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(20)
        |swapn
        |pop
        |push int32(19)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br337
        |@compare_br337:
        |push int32(35)
        |dupn
        |push int32(35)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(19)
        |swapn
        |pop
        |push int32(18)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br352
        |@compare_br352:
        |push int32(35)
        |dupn
        |push int8(4)
        |cast
        |push int32(34)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(18)
        |swapn
        |pop
        |push int32(17)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br369
        |@compare_br369:
        |push int32(33)
        |dupn
        |push int32(33)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(17)
        |swapn
        |pop
        |push int32(16)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br386
        |@compare_br386:
        |push int32(37)
        |dupn
        |push int32(37)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int8(4)
        |cast
        |push int32(36)
        |dupn
        |push int8(4)
        |cast
        |eq
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(35)
        |dupn
        |push int32(35)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(35)
        |dupn
        |push int8(4)
        |cast
        |push int32(34)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(33)
        |dupn
        |push int32(33)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int32(37)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(16)
        |swapn
        |pop
        |push int32(15)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br452
        |@compare_br452:
        |push int32(37)
        |dupn
        |push int8(4)
        |cast
        |push int32(36)
        |dupn
        |push int8(4)
        |cast
        |eq
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(15)
        |swapn
        |pop
        |push int32(14)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br469
        |@compare_br469:
        |push int32(35)
        |dupn
        |push int32(35)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(14)
        |swapn
        |pop
        |push int32(13)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br484
        |@compare_br484:
        |push int32(35)
        |dupn
        |push int8(4)
        |cast
        |push int32(34)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(13)
        |swapn
        |pop
        |push int32(12)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br501
        |@compare_br501:
        |push int32(33)
        |dupn
        |push int32(33)
        |dupn
        |eq
        |push int8(3)
        |cast
        |push int32(0)
        |eq
        |push int8(3)
        |cast
        |push int32(12)
        |swapn
        |pop
        |push int32(11)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br518
        |@compare_br518:
        |push int32(37)
        |dupn
        |push int32(37)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int8(4)
        |cast
        |push int32(36)
        |dupn
        |push int8(4)
        |cast
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(35)
        |dupn
        |push int32(35)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(35)
        |dupn
        |push int8(4)
        |cast
        |push int32(34)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(33)
        |dupn
        |push int32(33)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int32(37)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(11)
        |swapn
        |pop
        |push int32(10)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br566
        |@compare_br566:
        |push int32(37)
        |dupn
        |push int8(4)
        |cast
        |push int32(36)
        |dupn
        |push int8(4)
        |cast
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(10)
        |swapn
        |pop
        |push int32(9)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br580
        |@compare_br580:
        |push int32(35)
        |dupn
        |push int32(35)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(9)
        |swapn
        |pop
        |push int32(8)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br592
        |@compare_br592:
        |push int32(35)
        |dupn
        |push int8(4)
        |cast
        |push int32(34)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(8)
        |swapn
        |pop
        |push int32(7)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br606
        |@compare_br606:
        |push int32(33)
        |dupn
        |push int32(33)
        |dupn
        |swap
        |gt
        |push int8(3)
        |cast
        |push int32(7)
        |swapn
        |pop
        |push int32(6)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br620
        |@compare_br620:
        |push int32(37)
        |dupn
        |push int32(37)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int8(4)
        |cast
        |push int32(36)
        |dupn
        |push int8(4)
        |cast
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(35)
        |dupn
        |push int32(35)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(35)
        |dupn
        |push int8(4)
        |cast
        |push int32(34)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(33)
        |dupn
        |push int32(33)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(32)
        |swapn
        |pop
        |push int32(37)
        |dupn
        |push int32(37)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(6)
        |swapn
        |pop
        |push int32(5)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br668
        |@compare_br668:
        |push int32(37)
        |dupn
        |push int8(4)
        |cast
        |push int32(36)
        |dupn
        |push int8(4)
        |cast
        |swap
        |lt
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
        |jumpi @compare_br682
        |@compare_br682:
        |push int32(35)
        |dupn
        |push int32(35)
        |dupn
        |swap
        |lt
        |push int8(3)
        |cast
        |push int32(4)
        |swapn
        |pop
        |push int32(3)
        |dupn
        |push int8(9)
        |cast
        |not
        |push int8(3)
        |cast
        |push int32(1)
        |eq
        |jumpi @compare_br694
        |@compare_br694:
        |push int32(35)
        |dupn
        |push int8(4)
        |cast
        |push int32(34)
        |dupn
        |swap
        |lt
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
        |jumpi @compare_br708
        |@compare_br708:
        |push int32(33)
        |dupn
        |push int32(33)
        |dupn
        |swap
        |lt
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
        |jumpi @compare_br722
        |@compare_br722:
        |meta translator_mark "compare local vars clearing"
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
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |pop
        |meta translator_mark "end of compare method"
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
      """.stripMargin
        ).right.get
      )
    }
  }
}
