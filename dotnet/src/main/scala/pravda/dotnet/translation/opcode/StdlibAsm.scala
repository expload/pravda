package pravda.dotnet.translation.opcode
import com.google.protobuf.ByteString
import pravda.dotnet.translation.data.{MethodTranslation, OpCodeTranslation}
import pravda.vm.{Data, Opcodes}
import pravda.vm.asm.Operation

object StdlibAsm {

  private def stdlibFunc(name: String, ops: List[Operation]): MethodTranslation =
    MethodTranslation(
      "stdlib",
      name,
      forceAdd = false,
      List(
        OpCodeTranslation(
          List.empty,
          Operation.Label(s"stdlib_$name") :: ops
        )
      )
    )

  val stdlibFuncs: List[MethodTranslation] = List(
    stdlibFunc(
      "storage_get_default",
      dupn(2) ++
        cast(Data.Type.Bytes) ++
        dupn(4) ++
        List(
          Operation.Orphan(Opcodes.CONCAT),
          Operation.Orphan(Opcodes.SEXIST),
          Operation.JumpI(Some("get_default_if")),
          Operation.Orphan(Opcodes.SWAP),
          Operation.Orphan(Opcodes.POP),
          Operation.Orphan(Opcodes.SWAP),
          Operation.Orphan(Opcodes.POP),
          Operation.Orphan(Opcodes.RET),
          Operation.Label("get_default_if"),
          Operation.Orphan(Opcodes.POP)
        ) ++
        cast(Data.Type.Bytes) ++
        List(
          Operation.Orphan(Opcodes.SWAP),
          Operation.Orphan(Opcodes.CONCAT),
          Operation.Orphan(Opcodes.SGET),
          Operation.Orphan(Opcodes.RET)
        )
    ),
    stdlibFunc(
      "array_to_bytes",
      List(
        Operation(Opcodes.DUP),
        Operation(Opcodes.LENGTH),
        Operation.Push(Data.Primitive.Bytes(ByteString.EMPTY)),
        pushInt(0),
        Operation.Label("array_to_bytes_loop"),
        pushInt(4),
        Operation(Opcodes.DUPN),
        pushInt(2),
        Operation(Opcodes.DUPN),
        Operation(Opcodes.ARRAY_GET),
        pushType(Data.Type.Bytes),
        Operation(Opcodes.CAST),
        pushInt(3),
        Operation(Opcodes.DUPN),
        Operation(Opcodes.CONCAT),
        pushInt(3),
        Operation(Opcodes.SWAPN),
        Operation(Opcodes.POP),
        pushInt(1),
        Operation(Opcodes.ADD),
        Operation(Opcodes.DUP),
        pushInt(4),
        Operation(Opcodes.DUPN),
        Operation(Opcodes.GT),
        Operation.JumpI(Some("array_to_bytes_loop")),
        Operation(Opcodes.POP),
        Operation(Opcodes.SWAP),
        Operation(Opcodes.POP),
        Operation(Opcodes.SWAP),
        Operation(Opcodes.POP),
        Operation(Opcodes.RET)
      )
    )
  )
}
