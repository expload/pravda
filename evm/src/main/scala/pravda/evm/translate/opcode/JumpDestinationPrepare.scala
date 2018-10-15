package pravda.evm.translate.opcode

import pravda.evm.EVM
import pravda.evm.EVM.JumpDest
import pravda.vm.{Opcodes, asm}
import pravda.vm.asm.Operation.PushRef

object JumpDestinationPrepare {


  def evmOpToOps(op: EVM.Op): Either[String, List[asm.Operation]] =
    op match {
      case JumpDest(addr) => Right(List(PushRef(getNameByAddress(addr)),pushBigInt(BigInt(addr)),asm.Operation(Opcodes.SPUT)))
      case _ => Left(s"Require JUMPDEST opcode, but found $op")
    }
}
