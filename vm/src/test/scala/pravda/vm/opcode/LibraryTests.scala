package pravda.vm
package opcode

import utest._

import VmUtils._
import Opcodes._

import serialization._

import pravda.common.bytes.hex._

object LibraryTests extends TestSuite {

  val tests = Tests {
    'std - {
      val program = prog
        .opcode(PUSHX).put(1)
        .opcode(PUSHX).put(7)
        .opcode(PUSHX).put(43)
      val sum2 = program
        .opcode(LCALL).put("Math").put("sum").put(2)
      val sum3 = program
        .opcode(LCALL).put("Math").put("sum").put(3)

      exec(sum2) ==> stack(int32ToData(1), int32ToData(50))
      exec(sum3) ==> stack(int32ToData(51))

    }

    'udf - {
      val plusLen = prog.put("plus").length
      val multLen = prog.put("mult").length

      val udflib1 = prog.opcode(FTBL)
        .put(2)
        .put("plus").put(1 + 5 + plusLen + 5 + multLen + 5)
        .put("mult").put(1 + 5 + plusLen + 5 + multLen + 5 + 1 + 1)
        .opcode(I32ADD).opcode(RET)
        .opcode(I32MUL).opcode(RET)

      val udflib2 = prog.opcode(FTBL)
        .put(1)
        .put("plus").put(1 + 5 + plusLen + 5)
        .opcode(PUSHX).put(13)
        .opcode(I32ADD)
        .opcode(I32ADD)
        .opcode(RET)


      val address1 = data(hex"0405424e")
      val address2 = data(hex"0406424e")

      val wState = environment(address1 -> udflib1, address2 -> udflib2)

      val program = prog
        .opcode(PUSHX).put(7)
        .opcode(PUSHX).put(8)

      val plus1 = program.opcode(LCALL).put(address1).put("plus").put(2)
      val plus2 = program.opcode(LCALL).put(address2).put("plus").put(2)
      val mult1 = program.opcode(LCALL).put(address1).put("mult").put(2)

      exec(plus1, wState) ==> stack(int32ToData(15))
      exec(plus2, wState) ==> stack(int32ToData(28))
      exec(mult1, wState) ==> stack(int32ToData(56))

    }

    'udf2 - {

      val plusLen = prog.put("plus").length
      val funcLen = prog.put("func").length

      val address = data(hex"0405424e")

      val udflib = prog.opcode(FTBL)
        .put(2)
        .put("plus").put(1 + 5 + plusLen + 5 + funcLen + 5)
        .put("func").put(1 + 5 + plusLen + 5 + funcLen + 5 + 1 + 1)
        .opcode(I32ADD).opcode(RET)
        .opcode(DUP).opcode(LCALL).put(address).put("plus").put(2).opcode(PUSHX).put(1).opcode(I32ADD).opcode(RET)

      val wState = environment(address -> udflib)

      val double = prog
        .opcode(PUSHX).put(7)
        .opcode(LCALL).put(address).put("func").put(1)

      exec(double, wState) ==> stack(int32ToData(15))

    }
  }

}
