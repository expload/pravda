package io.mytc.sood.vm
package opcode

import VmUtils._
import Opcodes._

import org.scalatest.{FlatSpec, Matchers}

class BooleanOpcodesSpec extends FlatSpec with Matchers {

  "NOT opcode" should "negate the data" in {
    exec(prog.opcode(PUSHX).put(0.toByte).opcode(NOT)) shouldBe stack(data(1.toByte))
    exec(prog.opcode(PUSHX).put(1.toByte).opcode(NOT)) shouldBe stack(data(0.toByte))
    exec(prog.opcode(PUSHX).put(bytes(0xF0, 0x0F, 0x00, 0xFF)).opcode(NOT)) shouldBe stack(data(0.toByte))
    exec(prog.opcode(PUSHX).put(bytes(0x00, 0x00, 0x00, 0x00)).opcode(NOT)) shouldBe stack(data(1.toByte))
  }

  def op(operation: Byte)(d1: Array[Byte], d2: Array[Byte]) = prog
    .opcode(PUSHX).put(d1)
    .opcode(PUSHX).put(d2)
    .opcode(operation)

  val and = op(AND)(_, _)
  val or = op(OR)(_, _)
  val xor = op(XOR)(_, _)

  "AND opcode with 0" should "always give 0" in {
    def with0(d: Array[Byte]) = {
      exec(and(bytes(0), d)) shouldBe stack(data(0.toByte))
      exec(and(d, bytes(0))) shouldBe stack(data(0.toByte))
    }
    with0(bytes(0))
    with0(bytes(1))
    with0(bytes(1, 2, 3))
    with0(bytes(0, 0, 0))
  }

  "AND opcode with other values" should "do the and operation" in {
    exec(and(bytes(1), bytes(1))) shouldBe stack(fromBytes(1))
    exec(and(bytes(1, 0), bytes(1))) shouldBe stack(fromBytes(1))
    exec(and(bytes(1, 0, 0), bytes(0, 0))) shouldBe stack(fromBytes(0))
    exec(and(bytes(0, 1, 0), bytes(1, 0, 0, 0))) shouldBe stack(fromBytes(1))
    exec(and(bytes(0, 0, 0), bytes(0, 0))) shouldBe stack(fromBytes(0))
  }


  "OR opcode with 1" should "always give 1" in {
    def with1(d: Array[Byte]) = {
      exec(or(bytes(1), d)) shouldBe stack(data(1.toByte))
      exec(or(d, bytes(1))) shouldBe stack(data(1.toByte))
    }
    with1(bytes(0))
    with1(bytes(1))
    with1(bytes(1, 2, 3))
    with1(bytes(0, 0, 0))
  }

  "OR opcode with other values" should "do the and operation" in {
    exec(or(bytes(1), bytes(1))) shouldBe stack(fromBytes(1))
    exec(or(bytes(1, 0), bytes(1))) shouldBe stack(fromBytes(1))
    exec(or(bytes(1, 0, 0), bytes(0, 0))) shouldBe stack(fromBytes(1))
    exec(or(bytes(0, 1, 0), bytes(1, 0, 0, 0))) shouldBe stack(fromBytes(1))
    exec(or(bytes(0, 0, 0), bytes(0, 0))) shouldBe stack(fromBytes(0))
  }

  "XOR opcode" should "do the xor operaiton" in {
    exec(xor(bytes(1), bytes(1))) shouldBe stack(fromBytes(0))
    exec(xor(bytes(1, 0), bytes(1))) shouldBe stack(fromBytes(0))
    exec(xor(bytes(1, 0, 0), bytes(0, 0))) shouldBe stack(fromBytes(1))
    exec(xor(bytes(0, 0, 0), bytes(0, 1))) shouldBe stack(fromBytes(1))
    exec(xor(bytes(0, 1, 0), bytes(1, 0, 0, 0))) shouldBe stack(fromBytes(0))
    exec(xor(bytes(0, 0, 0), bytes(0, 0))) shouldBe stack(fromBytes(0))
  }

}
