package pravda.evm

package translate.opcode

import com.google.protobuf.ByteString
import pravda.vm.Data.Array.Int8Array
import pravda.vm.Data.Primitive.{BigInt, Ref}

import scala.collection.mutable.ArrayBuffer
import pravda.vm.{Data, Opcodes, VmSandbox}
import pravda.vm.asm
import utest._

object TestMemoryEmulation extends TestSuite {

  val tests = Tests {

    import VmSandbox.Expectations

    val precondition = VmSandbox.Preconditions(balances = Map.empty, `watts-limit` = 1000000L)
    val arr: List[Byte] =
      List(1, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4, 5, 6, 7, 8)

    def assemble(ops: Seq[asm.Operation]): ByteString = asm.PravdaAssembler.assemble(ops, saveLabels = true)

    "READ WORD" - {
      VmSandbox.run(
        precondition,
        assemble(StdlibAsm.createByteArray(arr) ++ List(pushInt(0)) ++ StdlibAsm.readWord)
      ) ==>
        Expectations(
          2446L,
          stack = ArrayBuffer(BigInt(scala.BigInt(1, arr.take(32).toArray))),
          heap = Map(Ref(0) -> Int8Array(arr.toBuffer))
        )

      VmSandbox.run(
        precondition,
        assemble(
          StdlibAsm.createByteArray(arr) ++
            List(pushInt(2)) ++ StdlibAsm.readWord)
      ) ==>
        Expectations(
          2446L,
          stack = ArrayBuffer(BigInt(scala.BigInt(1, arr.slice(2, 34).toArray))),
          heap = Map(Ref(0) -> Int8Array(arr.toBuffer))
        )
    }

    "WRITE WORD" - {

      VmSandbox.run(
        precondition,
        assemble(
          StdlibAsm.createByteArray(arr) ++
            List(
              pushInt(0)
            ) ++
            StdlibAsm.readWord ++ List(pushInt(arr.size),
                                       pushType(Data.Type.Int8),
                                       asm.Operation(Opcodes.NEW_ARRAY),
                                       asm.Operation(Opcodes.SWAP),
                                       pushInt(1)) ++ StdlibAsm.writeWord)
      ) ==>
        Expectations(
          4839,
          stack = ArrayBuffer(Ref(1)),
          heap = Map(
            Ref(0) -> Int8Array(arr.toBuffer),
            Ref(1) -> Int8Array((0.toByte :: arr.take(32) ::: List.fill[Byte](arr.size - 33)(0)).toBuffer)
          )
        )

      VmSandbox.run(
        precondition,
        assemble(
          StdlibAsm.createByteArray(arr) ++
            List(
              pushInt(0),
            ) ++
            StdlibAsm.readWord ++ List(pushInt(arr.size),
                                       pushType(Data.Type.Int8),
                                       asm.Operation(Opcodes.NEW_ARRAY),
                                       asm.Operation(Opcodes.SWAP),
                                       pushInt(31)) ++ StdlibAsm.writeWord)
      ) ==>
        Expectations(
          6514,
          stack = ArrayBuffer(Ref(2)),
          heap = Map(
            Ref(0) -> Int8Array(arr.toBuffer),
            Ref(1) -> Int8Array(List.fill[Byte](arr.size)(0).toBuffer),
            Ref(2) -> Int8Array(
              (List.fill[Byte](31)(0) ++ arr.take(32) ++ List.fill[Byte](arr.size * 2 - 31 - 32)(0)).toBuffer)
          )
        )
    }

    "EXPAND ARRAY" - {
      val x = VmSandbox.run(
        precondition,
        assemble(
          StdlibAsm.createByteArray(arr) ++
            List(
              pushInt(1),
            ) ++ StdlibAsm.expandArray
        )
      )

      x ==>
        Expectations(
          281,
          stack = ArrayBuffer(Ref(0)),
          heap = Map(
            Ref(0) -> Int8Array(arr.toBuffer),
          )
        )

      VmSandbox.run(
        precondition,
        assemble(
          StdlibAsm.createByteArray(arr) ++
            List(
              pushInt(arr.size - 2),
            ) ++ StdlibAsm.expandArray
        )
      ) ==>
        Expectations(
          1956,
          stack = ArrayBuffer(Ref(1)),
          heap = Map(
            Ref(0) -> Int8Array(arr.toBuffer),
            Ref(1) -> Int8Array((arr ++ List.fill[Byte](arr.size)(0)).toBuffer),
          )
        )
    }
  }
}
