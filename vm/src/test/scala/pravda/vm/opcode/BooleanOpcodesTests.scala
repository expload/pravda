package pravda.vm
package opcode

import VmUtils._
import Opcodes._

import utest._

object BooleanOpcodesTests extends TestSuite {

  val tests = Tests {
    'not - {
      exec(prog.opcode(PUSHX).put(0.toByte).opcode(NOT)) ==> stack(data(1.toByte))
      exec(prog.opcode(PUSHX).put(1.toByte).opcode(NOT)) ==> stack(data(0.toByte))
      exec(prog.opcode(PUSHX).put(bytes(0xF0, 0x0F, 0x00, 0xFF)).opcode(NOT)) ==> stack(data(0.toByte))
      exec(prog.opcode(PUSHX).put(bytes(0x00, 0x00, 0x00, 0x00)).opcode(NOT)) ==> stack(data(1.toByte))
    }

    def op(operation: Byte)(d1: Array[Byte], d2: Array[Byte]) = prog
      .opcode(PUSHX).put(d1)
      .opcode(PUSHX).put(d2)
      .opcode(operation)

    val and = op(AND)(_, _)
    val or = op(OR)(_, _)
    val xor = op(XOR)(_, _)

    'andWithFalse - {
      def with0(d: Array[Byte]) = {
        exec(and(bytes(0), d)) ==> stack(data(0.toByte))
        exec(and(d, bytes(0))) ==> stack(data(0.toByte))
      }

      with0(bytes(0))
      with0(bytes(1))
      with0(bytes(1, 2, 3))
      with0(bytes(0, 0, 0))
    }

    'and {
      exec(and(bytes(1), bytes(1))) ==> stack(data(1.toByte))
      exec(and(bytes(1, 0), bytes(1))) ==> stack(data(1.toByte))
      exec(and(bytes(1, 0, 0), bytes(0, 0))) ==> stack(data(0.toByte))
      exec(and(bytes(0, 1, 0), bytes(1, 0, 0, 0))) ==> stack(data(1.toByte))
      exec(and(bytes(0, 0, 0), bytes(0, 0))) ==> stack(data(0.toByte))
    }


    'orWirhTrue - {
      def with1(d: Array[Byte]) = {
        exec(or(bytes(1), d)) ==> stack(data(1.toByte))
        exec(or(d, bytes(1))) ==> stack(data(1.toByte))
      }

      with1(bytes(0))
      with1(bytes(1))
      with1(bytes(1, 2, 3))
      with1(bytes(0, 0, 0))
    }

    'or - {
      exec(or(bytes(1), bytes(1))) ==> stack(data(1.toByte))
      exec(or(bytes(1, 0), bytes(1))) ==> stack(data(1.toByte))
      exec(or(bytes(1, 0, 0), bytes(0, 0))) ==> stack(data(1.toByte))
      exec(or(bytes(0, 1, 0), bytes(1, 0, 0, 0))) ==> stack(data(1.toByte))
      exec(or(bytes(0, 0, 0), bytes(0, 0))) ==> stack(data(0.toByte))
    }

    'xor - {
      exec(xor(bytes(1), bytes(1))) ==> stack(data(0.toByte))
      exec(xor(bytes(1, 0), bytes(1))) ==> stack(data(0.toByte))
      exec(xor(bytes(1, 0, 0), bytes(0, 0))) ==> stack(data(1.toByte))
      exec(xor(bytes(0, 0, 0), bytes(0, 1))) ==> stack(data(1.toByte))
      exec(xor(bytes(0, 1, 0), bytes(1, 0, 0, 0))) ==> stack(data(0.toByte))
      exec(xor(bytes(0, 0, 0), bytes(0, 0))) ==> stack(data(0.toByte))
    }
  }
}
