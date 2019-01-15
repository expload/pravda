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

package pravda.evm

import fastparse.byte.all._

object EVM {
  sealed trait Op

  case object Stop                    extends Op
  case object Add                     extends Op
  case object Mul                     extends Op
  case object Sub                     extends Op
  case object Div                     extends Op
  case object SDiv                    extends Op
  case object Mod                     extends Op
  case object SMod                    extends Op
  case object AddMod                  extends Op
  case object MulMod                  extends Op
  case object Exp                     extends Op
  case object SignExtend              extends Op
  case object Lt                      extends Op
  case object Gt                      extends Op
  case object Slt                     extends Op
  case object Sgt                     extends Op
  case object Eq                      extends Op
  case object IsZero                  extends Op
  case object And                     extends Op
  case object Or                      extends Op
  case object Xor                     extends Op
  case object Not                     extends Op
  case object Byte                    extends Op
  case object Sha3                    extends Op
  case object Address                 extends Op
  case object Balance                 extends Op
  case object Origin                  extends Op
  case object Caller                  extends Op
  case object CallValue               extends Op
  case object CallDataLoad            extends Op
  case object CallDataSize            extends Op
  case object CallDataCopy            extends Op
  case object CodeSize                extends Op
  case object CodeCopy                extends Op
  case object GasPrice                extends Op
  case object ExtCodeSize             extends Op
  case object ExtCodeCopy             extends Op
  case object ReturnDataSize          extends Op
  case object ReturnDataCopy          extends Op
  case object BlockHash               extends Op
  case object CoinBase                extends Op
  case object Timestamp               extends Op
  case object Number                  extends Op
  case object Difficulty              extends Op
  case object GasLimit                extends Op
  case object Pop                     extends Op
  case object MLoad                   extends Op
  case object MStore                  extends Op
  case object MStore8                 extends Op
  case object SLoad                   extends Op
  case object SStore                  extends Op
  case object Jump                    extends Op
  case object JumpI                   extends Op
  case object Pc                      extends Op
  case object MSize                   extends Op
  case object Gas                     extends Op
  case object JumpDest                extends Op
  final case class Push(bytes: Bytes) extends Op
  final case class Dup(n: Int)        extends Op
  final case class Swap(n: Int)       extends Op
  final case class Log(n: Int)        extends Op
  case object Create                  extends Op
  case object Call                    extends Op
  case object CallCode                extends Op
  case object Return                  extends Op
  case object DelegateCall            extends Op
  case object StaticCall              extends Op
  case object Revert                  extends Op
  case object Invalid                 extends Op
  case object SelfDestruct            extends Op

  trait AddressedJumpOp extends Op {
    def addr: Int
  }
  final case class JumpDest(addr: Int)     extends Op
  case class SelfAddressedJump(addr: Int)  extends AddressedJumpOp
  case class SelfAddressedJumpI(addr: Int) extends AddressedJumpOp
  case class Jump(addr: Int, dest: Int)    extends AddressedJumpOp
  case class JumpI(addr: Int, dest: Int)   extends AddressedJumpOp

  case class MLoad(stackOffset: Int)   extends Op
  case class MStore(stackOffset: Int)  extends Op
  case class MStore8(stackOffset: Int) extends Op

  case class CallDataLoad(stackOffset: Int) extends Op
  case class CallDataSize(stackOffset: Int) extends Op

  case class Sha3(stackOffset: Int)   extends Op
  case class Return(stackOffset: Int) extends Op

  val singleOps: Map[Int, Op] = Map(
    0x00 -> Stop,
    0x01 -> Add,
    0x02 -> Mul,
    0x03 -> Sub,
    0x04 -> Div,
    0x05 -> SDiv,
    0x06 -> Mod,
    0x07 -> SMod,
    0x08 -> AddMod,
    0x09 -> MulMod,
    0x0a -> Exp,
    0x0b -> SignExtend,
    //
    0x10 -> Lt,
    0x11 -> Gt,
    0x12 -> Slt,
    0x13 -> Sgt,
    0x14 -> Eq,
    0x15 -> IsZero,
    0x16 -> And,
    0x17 -> Or,
    0x18 -> Xor,
    0x19 -> Not,
    0x1a -> Byte,
    //
    0x20 -> Sha3,
    //
    0x30 -> Address,
    0x31 -> Balance,
    0x32 -> Origin,
    0x33 -> Caller,
    0x34 -> CallValue,
    0x35 -> CallDataLoad,
    0x36 -> CallDataSize,
    0x37 -> CallDataCopy,
    0x38 -> CodeSize,
    0x39 -> CodeCopy,
    0x3a -> GasPrice,
    0x3b -> ExtCodeSize,
    0x3c -> ExtCodeCopy,
    0x3d -> ReturnDataSize,
    0x3e -> ReturnDataCopy,
    //
    0x40 -> BlockHash,
    0x41 -> CoinBase,
    0x42 -> Timestamp,
    0x43 -> Number,
    0x44 -> Difficulty,
    0x45 -> GasLimit,
    //
    0x50 -> Pop,
    0x51 -> MLoad,
    0x52 -> MStore,
    0x53 -> MStore8,
    0x54 -> SLoad,
    0x55 -> SStore,
    0x56 -> Jump,
    0x57 -> JumpI,
    0x58 -> Pc,
    0x59 -> MSize,
    0x5a -> Gas,
    0x5b -> JumpDest,
    //
    0xf0 -> Create,
    0xf1 -> Call,
    0xf2 -> CallCode,
    0xf3 -> Return,
    0xf4 -> DelegateCall,
    0xfa -> StaticCall,
    0xfd -> Revert,
    0xfe -> Invalid,
    0xff -> SelfDestruct
  )

  val rangeOps: List[(Range, Int => Op)] = List(
    (0x80 to 0x8f, i => Dup(i - 0x80 + 1)),
    (0x90 to 0x9f, i => Swap(i - 0x90 + 1)),
    (0xa0 to 0xa4, i => Log(i - 0xa0))
  )

  sealed trait AbiType

  sealed trait Fixed   extends AbiType
  sealed trait Dynamic extends AbiType

  final case class UInt(bytes: Int) extends Fixed
  final case class SInt(bytes: Int) extends Fixed

  final case object Bool        extends Fixed
  final case object Unsupported extends AbiType

}
