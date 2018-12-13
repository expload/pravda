/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.evm.translate.opcode

import pravda.evm.EVM
import pravda.evm.translate.Translator.Converted
import pravda.vm.asm
import pravda.vm._
import pravda.vm.asm.Operation
import pravda.evm.utils._

object SimpleTranslation {

  import pravda.evm.EVM._

  val pow2_256 = scala.BigInt(2).pow(256) - 1

  private def bigintOps(asmOps: List[asm.Operation]): List[Operation] =
    cast(Data.Type.BigInt) ++
      codeToOps(Opcodes.SWAP) ++
      cast(Data.Type.BigInt) ++
      codeToOps(Opcodes.SWAP) ++
      asmOps ++
      cast(Data.Type.Bytes)

  private def bigintOp(asmOp: asm.Operation): List[Operation] =
    bigintOps(List(asmOp))

  private val translate: PartialFunction[EVM.Op, List[asm.Operation]] = {
    case Push(bytes) => List(Operation.Push(evmWord(bytes.toArray)))

    case Pop => codeToOps(Opcodes.POP)

    case Add => bigintOp(Operation(Opcodes.ADD)) //FIXME result % 2^256
    case Mul => bigintOp(Operation(Opcodes.MUL))
    case Div => bigintOp(Operation(Opcodes.DIV)) //FIXME 0 if stack[1] == 0 othervise s[0] / s[1]
    case Mod => bigintOp(Operation(Opcodes.MOD)) //FIXME 0 if stack[1] == 0 othervise s[0] % s[1]
    case Sub => bigintOps(sub) //FIXME result & (2^256 - 1)
//    case AddMod =>
//      dupn(3) ::: codeToOps(Opcodes.SWAP, Opcodes.MOD, Opcodes.SWAP) ::: dupn(3) :::
//        codeToOps(Opcodes.SWAP, Opcodes.MOD, Opcodes.ADD, Opcodes.MOD)
//    case MulMod =>
//      dupn(3) ::: codeToOps(Opcodes.SWAP, Opcodes.MOD, Opcodes.SWAP) ::: dupn(3) :::
//        codeToOps(Opcodes.SWAP, Opcodes.MOD, Opcodes.MUL, Opcodes.MOD)

    //  case Not => codeToOps(Opcodes.NOT) //TODO (2^256 - 1) - s[0]

    case And => bigintOp(Operation(Opcodes.AND))
    case Or  => bigintOp(Operation(Opcodes.OR))
    case Xor => bigintOp(Operation(Opcodes.XOR))

    case Byte =>
      cast(Data.Type.BigInt) ++
        List(pushBigInt(31)) ++
        sub ++
        List(pushBigInt(8)) ++
        codeToOps(Opcodes.MUL) ++
        List(pushBigInt(2)) ++
        callExp ++
        codeToOps(Opcodes.SWAP) ++
        codeToOps(Opcodes.DIV) ++
        List(pushBigInt(0xff)) ++
        codeToOps(Opcodes.AND) ++
        cast(Data.Type.Bytes)

    case IsZero => pushBytes(Array.fill(32)(0)) :: codeToOps(Opcodes.EQ) ++ cast(Data.Type.Bytes)
    case Lt     => bigintOp(Operation(Opcodes.LT))
    case Gt     => bigintOp(Operation(Opcodes.GT))
    case Eq     => bigintOp(Operation(Opcodes.EQ))

    case Jump(_, dest)  => codeToOps(Opcodes.POP) ++ List(Operation.Jump(Some(nameByAddress(dest))))
    case JumpI(_, dest) => jumpi(dest)

    case Stop => codeToOps(Opcodes.STOP)

    case Dup(n)  => if (n > 1) dupn(n) else codeToOps(Opcodes.DUP)
    case Swap(n) => if (n > 1) swapn(n + 1) else codeToOps(Opcodes.SWAP)

    case Balance => codeToOps(Opcodes.BALANCE)
    case Address => codeToOps(Opcodes.PADDR)

    case JumpDest(address) => asm.Operation.Label(nameByAddress(address)) :: Nil

    case SStore => codeToOps(Opcodes.SPUT)
    case SLoad  => codeToOps(Opcodes.SGET)

    case MLoad(size) =>
      cast(Data.Type.BigInt) ::: pushInt(size + 1) :: codeToOps(Opcodes.DUPN) ::: List(
        Operation.Push(Data.Primitive.Int8(6)),
        Operation(Opcodes.SCALL))
    case MStore(size) =>
      cast(Data.Type.BigInt) ::: pushInt(size + 1) :: codeToOps(Opcodes.DUPN) ::: List(
        Operation.Push(Data.Primitive.Int8(7)),
        Operation(Opcodes.SCALL)) :::
        pushInt(size) :: codeToOps(Opcodes.SWAPN, Opcodes.POP)

    case MStore8(stackSize) => List(Operation.Meta(Meta.Custom(s"MStore8_$stackSize")))

    case Not    => pushBigInt(pow2_256) :: sub ::: Nil
    case Revert => List(Operation.Push(Data.Primitive.Utf8("Revert")), Operation(Opcodes.THROW))
    case Return =>
      cast(Data.Type.BigInt) :::
        List(Operation(Opcodes.SWAP)) :::
        cast(Data.Type.BigInt) :::
        List(Operation(Opcodes.SWAP)) :::
        List(
        pushInt(4),
        Operation(Opcodes.DUPN),
        pushInt(8),
        Operation(Opcodes.SCALL),
        Operation(Opcodes.SWAP),
        Operation(Opcodes.POP),
        Operation(Opcodes.SWAP),
        Operation(Opcodes.POP),
        Operation(Opcodes.STOP)
      )

    case CallValue    => pushBigInt(scala.BigInt(10)) :: cast(Data.Type.Bytes)
    case CallDataSize =>
      List(pushBytes(Array(0x04)))
    case CallDataLoad =>
      codeToOps(Opcodes.POP) ::: pushBytes(Array(0x12, 0x34)) :: Nil
    case Invalid      => codeToOps(Opcodes.STOP)
  }

  def evmOpToOps(op: EVM.Op): Converted =
    translate.lift(op).toRight(op)
}
