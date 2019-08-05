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

import pravda.common.vm.Opcodes
import pravda.evm.EVM
import pravda.evm.translate.Translator.Converted
import pravda.vm.asm
import pravda.common.vm._
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
      cast(Data.Type.Bytes) ++
      List(Operation.Push(Data.Primitive.Int8(8)), Operation(Opcodes.SCALL))

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

    case And => codeToOps(Opcodes.AND)
    case Or  => codeToOps(Opcodes.OR)
    case Xor => codeToOps(Opcodes.XOR)

    case Byte =>
      List(
        pushBigInt(31) :: Nil,
        sub,
        pushBigInt(8) :: Nil,
        codeToOps(Opcodes.MUL),
        pushBigInt(2) :: Nil,
        callExp,
        codeToOps(Opcodes.SWAP),
        codeToOps(Opcodes.DIV),
        pushBigInt(0xff) :: Nil,
        codeToOps(Opcodes.AND)
      ).flatten

    case IsZero =>
      pushBytes(Array.fill[Byte](32)(0)) :: codeToOps(Opcodes.EQ) :::
        cast(Data.Type.Bytes) ++ List(Operation.Push(Data.Primitive.Int8(8)), Operation(Opcodes.SCALL))
    case Lt             => bigintOp(Operation(Opcodes.LT))
    case Gt             => bigintOp(Operation(Opcodes.GT))
    case Eq             => bigintOp(Operation(Opcodes.EQ))
    case Jump(_, dest)  => codeToOps(Opcodes.POP) ::: Operation.Jump(Some(nameByAddress(dest))) :: Nil
    case JumpI(_, dest) => jumpi(dest)

    case Stop => codeToOps(Opcodes.POP, Opcodes.POP, Opcodes.POP, Opcodes.STOP)

    case Dup(n)  => if (n > 1) dupn(n) else codeToOps(Opcodes.DUP)
    case Swap(n) => if (n > 1) swapn(n + 1) else codeToOps(Opcodes.SWAP)

    case Balance => codeToOps(Opcodes.BALANCE)
    case Address => codeToOps(Opcodes.PADDR)

    case JumpDest(address) => asm.Operation.Label(nameByAddress(address)) :: Nil

    case SStore => codeToOps(Opcodes.SPUT)
    case SLoad  => List(Operation.Call(Some("stdlib_evm_sget")))

    case MLoad(offset) =>
      pushBigInt(scala.BigInt(32)) :: codeToOps(Opcodes.SWAP) :::
        cast(Data.Type.BigInt) :::
        pushInt(offset + 1) :: codeToOps(Opcodes.DUPN) :::
        List(Operation.Push(Data.Primitive.Int8(6)), Operation(Opcodes.SCALL))
    case MStore(offset) =>
      codeToOps(Opcodes.SWAP) ::: List(pushInt8(8)) ::: codeToOps(Opcodes.SCALL, Opcodes.SWAP) :::
        cast(Data.Type.BigInt) ::: pushInt(offset) :: codeToOps(Opcodes.DUPN) :::
        List(pushInt8(7), Operation(Opcodes.SCALL)) :::
        pushInt(offset - 1) :: codeToOps(Opcodes.SWAPN, Opcodes.POP)

    case MStore8(offset) => List(Operation.Meta(Meta.Custom(s"MStore8_$offset")))

    case Not    => pushBigInt(pow2_256) :: sub ::: Nil
    case Revert => List(Operation.Push(Data.Primitive.Utf8("Revert")), Operation(Opcodes.THROW))
    case Return =>
      cast(Data.Type.BigInt) :::
        List(Operation(Opcodes.SWAP)) :::
        cast(Data.Type.BigInt) :::
        List(Operation(Opcodes.SWAP)) :::
        List(
        pushInt(3),
        Operation(Opcodes.DUPN),
        pushInt8(6),
        Operation(Opcodes.SCALL),
        Operation(Opcodes.SWAP),
        Operation(Opcodes.POP),
        Operation(Opcodes.SWAP),
        Operation(Opcodes.POP),
        Operation(Opcodes.SWAP),
        Operation.Jump(Some("convert_result"))
      )

    case CallValue => List(pushBytes(Array(0x01)))
    case CallDataSize(offset) =>
      pushInt(offset + 1) :: codeToOps(Opcodes.DUPN, Opcodes.LENGTH)
    case CallDataLoad(offset) =>
      cast(Data.Type.BigInt) :::
        pushInt(offset + 1) ::
        codeToOps(Opcodes.DUPN, Opcodes.SWAP, Opcodes.DUP) :::
        pushInt(32) ::
        codeToOps(Opcodes.ADD, Opcodes.SWAP, Opcodes.SLICE)
    case Invalid => List(Operation.Push(Data.Primitive.Utf8("Invalid")), Operation(Opcodes.THROW))
    case Sha3(offset) =>
      cast(Data.Type.BigInt) :::
        codeToOps(Opcodes.SWAP) :::
        cast(Data.Type.BigInt) :::
        codeToOps(Opcodes.SWAP) :::
        pushInt(offset) ::
        codeToOps(Opcodes.DUPN) ::: List(
        pushInt8(6),
        Operation(Opcodes.SCALL),
        pushInt8(9),
        Operation(Opcodes.SCALL)
      )

    case Caller => codeToOps(Opcodes.FROM)
  }

  def evmOpToOps(op: EVM.Op): Converted =
    translate.lift(op).toRight(op)
}
