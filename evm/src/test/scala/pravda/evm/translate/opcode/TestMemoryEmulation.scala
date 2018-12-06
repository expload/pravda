package pravda.evm.translate.opcode

import pravda.evm.Preconditions
import pravda.evm.evm._
import pravda.vm.Data.Array.Int8Array
import pravda.vm.Data.Primitive.{BigInt, Ref}

import scala.collection.mutable.ArrayBuffer
import pravda.vm.VmSuiteData.Expectations
import pravda.vm.{Data, Opcodes}
import pravda.vm.asm.Operation
import utest._

object TestMemoryEmulation extends TestSuite {

  val tests = Tests {
    //FIXME when memory usage will be fixed. All tests will be incorrect
    val precondition = Preconditions(balances = Map.empty, `watts-limit` = 1000000L)

    "READ WORD" - {
      val x = run(
        Right(
          List(
            pushInt(35),
            pushType(Data.Type.Int8),
            Operation(Opcodes.NEW_ARRAY),
            Operation(Opcodes.DUP),
            pushByte(100),
            pushInt(31),
            Operation(Opcodes.ARRAY_MUT),
            pushInt(0),
          ) ++ StdlibAsm.readWord),
        precondition
      )

      x ==> Right(
        expectations(
          2310L,
          stack = ArrayBuffer(BigInt(scala.BigInt(100))),
          heap = Map(Ref(0) -> Int8Array(ArrayBuffer(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0)))
        ))
    }

    "WRITE WORD" - {

      val x: Either[String, Expectations] = run(
        Right(
          List(
            pushInt(35),
            pushType(Data.Type.Int8),
            Operation(Opcodes.NEW_ARRAY),
            Operation(Opcodes.DUP),
            pushByte(100),
            pushInt(0),
            Operation(Opcodes.ARRAY_MUT),
            Operation(Opcodes.DUP),
            pushByte(3),
            pushInt(3),
            Operation(Opcodes.ARRAY_MUT),
            Operation(Opcodes.DUP),
            pushByte(6),
            pushInt(6),
            Operation(Opcodes.ARRAY_MUT),
            Operation(Opcodes.DUP),
            pushByte(31),
            pushInt(31),
            Operation(Opcodes.ARRAY_MUT),
            pushInt(0),
          ) ++
            StdlibAsm.sliceArray ++ StdlibAsm.byteStringToBigint ++ List(pushInt(35),
                                                                         pushType(Data.Type.Int8),
                                                                         Operation(Opcodes.NEW_ARRAY),
                                                                         Operation(Opcodes.SWAP),
                                                                         pushInt(1)) ++ StdlibAsm.writeWord),
        precondition
      )

      println(x)
      x match {
        case Right(v) =>
          v.stack.reverse.foreach(println)
          v.heap.foreach(println)
          println(v.error)
        case _ =>
      }

      x ==> Right(
        expectations(
          4673,
          stack = ArrayBuffer(),
          heap = Map(
            Ref(0) -> Int8Array(ArrayBuffer(100, 0, 0, 3, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 31, 0, 0, 0)),
            Ref(1) -> Int8Array(ArrayBuffer(0, 100, 0, 0, 3, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 31, 0, 0))
          )
        ))
    }

  }
}
