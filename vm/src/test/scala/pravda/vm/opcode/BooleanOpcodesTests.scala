package pravda.vm
package opcode

import VmUtils._
import Opcodes._

import utest._

import pravda.common.bytes.hex._

object BooleanOpcodesTests extends TestSuite {

  val tests = Tests {
    'not - {
      stackOfExec(prog.opcode(PUSHX).put(0.toByte).opcode(NOT)) ==> stack(data(1.toByte))
      stackOfExec(prog.opcode(PUSHX).put(1.toByte).opcode(NOT)) ==> stack(data(0.toByte))
      stackOfExec(prog.opcode(PUSHX).put(hex"F00F00FF").opcode(NOT)) ==> stack(data(0.toByte))
      stackOfExec(prog.opcode(PUSHX).put(hex"00000000").opcode(NOT)) ==> stack(data(1.toByte))
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
        stackOfExec(and(hex"00", d)) ==> stack(data(0.toByte))
        stackOfExec(and(d, hex"00")) ==> stack(data(0.toByte))
      }

      with0(hex"00")
      with0(hex"01")
      with0(hex"010203")
      with0(hex"000000")
    }

    'and {
      stackOfExec(and(hex"01", hex"01")) ==> stack(data(1.toByte))
      stackOfExec(and(hex"0100", hex"01")) ==> stack(data(1.toByte))
      stackOfExec(and(hex"010000", hex"0000")) ==> stack(data(0.toByte))
      stackOfExec(and(hex"000100", hex"010000")) ==> stack(data(1.toByte))
      stackOfExec(and(hex"000000", hex"0000")) ==> stack(data(0.toByte))
    }


    'orWirhTrue - {
      def with1(d: Array[Byte]) = {
        stackOfExec(or(hex"01", d)) ==> stack(data(1.toByte))
        stackOfExec(or(d, hex"01")) ==> stack(data(1.toByte))
      }

      with1(hex"00")
      with1(hex"01")
      with1(hex"010203")
      with1(hex"000000")

    }

    'or - {
      stackOfExec(or(hex"01", hex"01")) ==> stack(data(1.toByte))
      stackOfExec(or(hex"0100", hex"01")) ==> stack(data(1.toByte))
      stackOfExec(or(hex"010000", hex"0000")) ==> stack(data(1.toByte))
      stackOfExec(or(hex"000100", hex"01000000")) ==> stack(data(1.toByte))
      stackOfExec(or(hex"000000", hex"0000")) ==> stack(data(0.toByte))
    }

    'xor - {
      stackOfExec(xor(hex"01", hex"01")) ==> stack(data(0.toByte))
      stackOfExec(xor(hex"0100", hex"01")) ==> stack(data(0.toByte))
      stackOfExec(xor(hex"010000", hex"0000")) ==> stack(data(1.toByte))
      stackOfExec(xor(hex"000000", hex"0001")) ==> stack(data(1.toByte))
      stackOfExec(xor(hex"000100", hex"01000000")) ==> stack(data(0.toByte))
      stackOfExec(xor(hex"000000", hex"0000")) ==> stack(data(0.toByte))
    }
  }
}
