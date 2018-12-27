package pravda.evm.translate.opcode

import com.google.protobuf.ByteString
import pravda.vm.asm.Operation
import pravda.vm.{Data, Opcodes}

object StdlibAsm {
  final case class Function(name: String, code: List[Operation])

  private def stdlibFunc(name: String, ops: List[Operation]): Function = {
    val fname = s"stdlib_$name"
    Function(fname,
             List(Operation.Label(fname)) ++
               ops ++ List(Operation(Opcodes.RET)))
  }

  val stdlibFuncs = List(
    stdlibFunc(
      "evm_sget",
      List(
        Operation(Opcodes.DUP),
        Operation(Opcodes.SEXIST),
        Operation.JumpI(Some("stdlib_evm_sget_non_zero")),
        Operation(Opcodes.POP),
        Operation.Push(Data.Primitive.Bytes(ByteString.copyFrom(Array.fill[Byte](32)(0)))),
        Operation(Opcodes.RET),
        Operation.Label("stdlib_evm_sget_non_zero"),
        Operation(Opcodes.SGET)
      )
    )
  )

}
