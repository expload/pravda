package io.mytc.sood.vm.libs

import io.mytc.sood.vm._
import org.scalatest.{FlatSpec, Matchers}
import io.mytc.sood.vm.VmUtils._
import io.mytc.sood.vm.Opcodes._
import serialization._

class TypedSpec extends FlatSpec with Matchers {
  "typedI32" should "produce correct words" in {
    val program = prog
      .opcode(PUSHX)
      .put(2)
      .opcode(PUSHX)
      .put(3)
      .opcode(PUSHX)
      .put(0x0abcdef1)
    val typedi32 = program
      .opcode(LCALL)
      .put("Typed")
      .put("typedI32")
      .put(3)

    exec(typedi32) shouldBe stack(bytes(1, 0, 0, 0, 2), bytes(1, 0, 0, 0, 3), bytes(1, 0x0a, 0xbc, 0xde, 0xf1))
  }

  "typedR64" should "produce correct words" in {
    val program = prog
      .opcode(PUSHX)
      .put(1.0)
      .opcode(PUSHX)
      .put(math.Pi)

    exec(program) shouldBe stack(bytes(0x3f, 0xf0, 0, 0, 0, 0, 0, 0),
                                 bytes(0x40, 0x09, 0x21, 0xfb, 0x54, 0x44, 0x2d, 0x18))

    val typedr64 = program
      .opcode(LCALL)
      .put("Typed")
      .put("typedR64")
      .put(2)

    exec(typedr64) shouldBe stack(bytes(2, 0x3f, 0xf0, 0, 0, 0, 0, 0, 0),
                                  bytes(2, 0x40, 0x09, 0x21, 0xfb, 0x54, 0x44, 0x2d, 0x18))
  }

  private def testTypedArithmetics(i1: Int,
                                   i2: Int,
                                   f1: Double,
                                   f2: Double,
                                   typedFunc: String,
                                   iFunc: (Int, Int) => Int,
                                   fFunc: (Double, Double) => Double): Unit = {

    typedFunc should "work for ints and doubles" in {

      val programI = prog
        .opcode(PUSHX)
        .put(i1)
        .opcode(PUSHX)
        .put(i2)

      val typedFuncI = programI
        .opcode(LCALL)
        .put("Typed")
        .put("typedI32")
        .put(2)
        .opcode(LCALL)
        .put("Typed")
        .put(typedFunc)
        .put(2)

      val execRes = exec(typedFuncI)

      execRes shouldBe stack(1.toByte +: int32ToData(iFunc(i1, i2)))

      val programR = prog
        .opcode(PUSHX)
        .put(f1)
        .opcode(PUSHX)
        .put(f2)

      val typedAddR = programR
        .opcode(LCALL)
        .put("Typed")
        .put("typedR64")
        .put(2)
        .opcode(LCALL)
        .put("Typed")
        .put(typedFunc)
        .put(2)

      exec(typedAddR) shouldBe stack(2.toByte +: doubleToData(fFunc(f1, f2)))
    }
  }

  testTypedArithmetics(1, 2, 1.0, 2.0, "typedAdd", _ + _, _ + _)

  testTypedArithmetics(1, 2, 1.0, 2.0, "typedMul", _ * _, _ * _)

  testTypedArithmetics(1, 2, 1.0, 2.0, "typedDiv", _ / _, _ / _)

  testTypedArithmetics(1, 2, 1.0, 2.0, "typedMod", _ % _, _ % _)
}
