package pravda.vm.libs

import com.google.protobuf.ByteString
import pravda.vm._
import pravda.vm.VmUtils._
import pravda.vm.Opcodes._
import serialization._

import utest._

object TypedTests extends TestSuite {
  val tests = Tests {
    'typedI32 - {
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

      exec(typedi32) ==> stack(data(1.toByte, 0.toByte, 0.toByte, 0.toByte, 2.toByte),
        data(1.toByte, 0.toByte, 0.toByte, 0.toByte, 3.toByte),
        data(1.toByte, 0x0a.toByte, 0xbc.toByte, 0xde.toByte, 0xf1.toByte))
    }

    'typedR64 - {
      val program = prog
        .opcode(PUSHX)
        .put(1.0)
        .opcode(PUSHX)
        .put(math.Pi)

      exec(program) ==> stack(data(0x3f.toByte, 0xf0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte),
        data(0x40.toByte, 0x09.toByte, 0x21.toByte, 0xfb.toByte, 0x54.toByte, 0x44.toByte, 0x2d.toByte, 0x18.toByte))

      val typedr64 = program
        .opcode(LCALL)
        .put("Typed")
        .put("typedR64")
        .put(2)

      exec(typedr64) ==> stack(data(2.toByte, 0x3f.toByte, 0xf0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte),
        data(2.toByte, 0x40.toByte, 0x09.toByte, 0x21.toByte, 0xfb.toByte, 0x54.toByte, 0x44.toByte, 0x2d.toByte, 0x18.toByte))
    }

    def testTypedArithmetics(i1: Int,
                                     i2: Int,
                                     f1: Double,
                                     f2: Double,
                                     typedFunc: String,
                                     iFunc: (Int, Int) => Int,
                                     fFunc: (Double, Double) => Double): Unit

    =
    {

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

      execRes ==> stack(ByteString.copyFrom(Array(1.toByte)) concat int32ToData(iFunc(i1, i2)))

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

      exec(typedAddR) ==> stack(ByteString.copyFrom(Array(2.toByte)) concat doubleToData(fFunc(f1, f2)))
    }

    * - testTypedArithmetics(1, 2, 1.0, 2.0, "typedAdd", _ + _, _ + _)

    * - testTypedArithmetics(1, 2, 1.0, 2.0, "typedMul", _ * _, _ * _)

    * - testTypedArithmetics(1, 2, 1.0, 2.0, "typedDiv", _ / _, _ / _)

    * - testTypedArithmetics(1, 2, 1.0, 2.0, "typedMod", _ % _, _ % _)
  }
}
