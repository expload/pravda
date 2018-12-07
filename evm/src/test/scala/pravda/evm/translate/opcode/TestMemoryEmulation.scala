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
    val arr: List[Byte] =
      List(1, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4, 5, 6, 7, 8)

    "READ WORD" - {
      val x = run(
        Right(
          StdlibAsm.createByteArray(arr) ++
            List(pushInt(0)) ++ StdlibAsm.readWord),
        precondition
      )

      x ==> Right(
        expectations(
          2446L,
          stack = ArrayBuffer(BigInt(scala.BigInt(1, arr.take(32).toArray))),
          heap = Map(Ref(0) -> Int8Array(arr.toBuffer))
        ))

      run(
        Right(
          StdlibAsm.createByteArray(arr) ++
            List(pushInt(2), ) ++ StdlibAsm.readWord),
        precondition
      ) ==> Right(
        expectations(
          2446L,
          stack = ArrayBuffer(BigInt(scala.BigInt(1, arr.drop(2).take(32).toArray))),
          heap = Map(Ref(0) -> Int8Array(arr.toBuffer))
        ))
    }

    "WRITE WORD" - {

      val x: Either[String, Expectations] = run(
        Right(
          StdlibAsm.createByteArray(arr) ++
            List(
              pushInt(0),
            ) ++
            StdlibAsm.readWord ++ List(pushInt(arr.size),
                                       pushType(Data.Type.Int8),
                                       Operation(Opcodes.NEW_ARRAY),
                                       Operation(Opcodes.SWAP),
                                       pushInt(1)) ++ StdlibAsm.writeWord),
        precondition
      )

      x ==> Right(
        expectations(
          4840,
          stack = ArrayBuffer(),
          heap = Map(
            Ref(0) -> Int8Array(arr.toBuffer),
            Ref(1) -> Int8Array((0.toByte :: arr.take(32) ::: List.fill[Byte](arr.size - 33)(0)).toBuffer)
          )
        ))

      run(
        Right(
          StdlibAsm.createByteArray(arr) ++
            List(
              pushInt(0),
            ) ++
            StdlibAsm.readWord ++ List(pushInt(arr.size),
                                       pushType(Data.Type.Int8),
                                       Operation(Opcodes.NEW_ARRAY),
                                       Operation(Opcodes.SWAP),
                                       pushInt(31)) ++ StdlibAsm.writeWord),
        precondition
      ) ==> Right(
        expectations(
          6515,
          stack = ArrayBuffer(),
          heap = Map(
            Ref(0) -> Int8Array(arr.toBuffer),
            Ref(1) -> Int8Array(List.fill[Byte](arr.size)(0).toBuffer),
            Ref(2) -> Int8Array(
              (List.fill[Byte](31)(0) ++ arr.take(32) ++ List.fill[Byte](arr.size * 2 - 31 - 32)(0)).toBuffer)
          )
        ))
    }

    "EXPAND ARRAY" - {
      val x = run(
        Right(
          StdlibAsm.createByteArray(arr) ++
            List(
              pushInt(1),
            ) ++ StdlibAsm.expandArray
        ),
        precondition
      )

      x ==> Right(
        expectations(
          281,
          stack = ArrayBuffer(Ref(0)),
          heap = Map(
            Ref(0) -> Int8Array(arr.toBuffer),
          )
        ))

      run(
        Right(
          StdlibAsm.createByteArray(arr) ++
            List(
              pushInt(arr.size - 2),
            ) ++ StdlibAsm.expandArray
        ),
        precondition
      ) ==> Right(
        expectations(
          1956,
          stack = ArrayBuffer(Ref(1)),
          heap = Map(
            Ref(0) -> Int8Array(arr.toBuffer),
            Ref(1) -> Int8Array((arr ++ List.fill[Byte](arr.size)(0)).toBuffer),
          )
        ))
    }
  }

}
