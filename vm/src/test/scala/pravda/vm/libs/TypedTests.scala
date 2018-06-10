//package pravda.vm.libs
//
//import com.google.protobuf.ByteString
//import pravda.vm._
//import pravda.vm.VmUtils._
//import pravda.vm.Opcodes._
//import serialization._
//
//import utest._
//
//import pravda.common.bytes.hex._
//
//object TypedTests extends TestSuite {
//  val tests = Tests {
//    'typedI32 - {
//      val program = prog
//        .opcode(PUSHX)
//        .put(2)
//        .opcode(PUSHX)
//        .put(3)
//        .opcode(PUSHX)
//        .put(0x0abcdef1)
//      val typedi32 = program
//        .opcode(LCALL)
//        .put("Typed")
//        .put("typedI32")
//        .put(3)
//
//      stackOfExec(typedi32) ==> stack(data(hex"01 00000002"),
//        data(hex"01 00000003"),
//        data(hex"01 0abcdef1"))
//    }
//
//    'typedR64 - {
//      val program = prog
//        .opcode(PUSHX)
//        .put(1.0)
//        .opcode(PUSHX)
//        .put(math.Pi)
//
//      stackOfExec(program) ==> stack(data(hex"3f f0 00 00 00 00 00 00"),
//        data(hex"40 09 21 fb 54 44 2d 18"))
//
//      val typedr64 = program
//        .opcode(LCALL)
//        .put("Typed")
//        .put("typedR64")
//        .put(2)
//
//      stackOfExec(typedr64) ==> stack(data(hex"02 3f f0 00 00 00 00 00 00"),
//        data(hex"02 40 09 21 fb 54 44 2d 18"))
//    }
//
//    def testTypedArithmetics(i1: Int,
//                                     i2: Int,
//                                     f1: Double,
//                                     f2: Double,
//                                     typedFunc: String,
//                                     iFunc: (Int, Int) => Int,
//                                     fFunc: (Double, Double) => Double): Unit
//
//    =
//    {
//
//      val programI = prog
//        .opcode(PUSHX)
//        .put(i1)
//        .opcode(PUSHX)
//        .put(i2)
//
//      val typedFuncI = programI
//        .opcode(LCALL)
//        .put("Typed")
//        .put("typedI32")
//        .put(2)
//        .opcode(LCALL)
//        .put("Typed")
//        .put(typedFunc)
//        .put(2)
//
//      val execRes = stackOfExec(typedFuncI)
//
//      execRes ==> stack(ByteString.copyFrom(Array(1.toByte)) concat int32ToData(iFunc(i1, i2)))
//
//      val programR = prog
//        .opcode(PUSHX)
//        .put(f1)
//        .opcode(PUSHX)
//        .put(f2)
//
//      val typedAddR = programR
//        .opcode(LCALL)
//        .put("Typed")
//        .put("typedR64")
//        .put(2)
//        .opcode(LCALL)
//        .put("Typed")
//        .put(typedFunc)
//        .put(2)
//
//      stackOfExec(typedAddR) ==> stack(ByteString.copyFrom(Array(2.toByte)) concat doubleToData(fFunc(f1, f2)))
//    }
//
//    * - testTypedArithmetics(1, 2, 1.0, 2.0, "typedAdd", _ + _, _ + _)
//
//    * - testTypedArithmetics(1, 2, 1.0, 2.0, "typedMul", _ * _, _ * _)
//
//    * - testTypedArithmetics(1, 2, 1.0, 2.0, "typedDiv", _ / _, _ / _)
//
//    * - testTypedArithmetics(1, 2, 1.0, 2.0, "typedMod", _ % _, _ % _)
//  }
//}
