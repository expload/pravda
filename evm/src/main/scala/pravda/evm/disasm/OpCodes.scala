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

package pravda.evm.disasm

import pravda.evm.EVM
import pravda.evm.EVM._

object OpCodes {

  val stackReadCount: PartialFunction[EVM.Op, Int] = {
    case Dup(n)  => n
    case Swap(n) => n + 1
    case Log(n)  => n + 2

    case Not                  => 1
    case IsZero               => 1
    case SignExtend           => 1
    case Balance              => 1
    case CallDataLoad         => 1
    case BlockHash            => 1
    case ExtCodeSize          => 1
    case Pop                  => 1
    case MLoad                => 1
    case SLoad                => 1
    case Jump                 => 1
    case SelfDestruct         => 1
    case SelfAddressedJump(_) => 1

    case Add                   => 2
    case Mul                   => 2
    case Sub                   => 2
    case Div                   => 2
    case SDiv                  => 2
    case Mod                   => 2
    case SMod                  => 2
    case Exp                   => 2
    case Lt                    => 2
    case Gt                    => 2
    case Slt                   => 2
    case Sgt                   => 2
    case Eq                    => 2
    case And                   => 2
    case Or                    => 2
    case Xor                   => 2
    case Byte                  => 2
    case Sha3                  => 2
    case MStore                => 2
    case MStore8               => 2
    case SStore                => 2
    case JumpI                 => 2
    case SelfAddressedJumpI(_) => 2
    case Return                => 2

    case AddMod       => 3
    case MulMod       => 3
    case CallDataCopy => 3
    case CodeCopy     => 3
    case Create       => 3

    case ExtCodeCopy => 4

    case DelegateCall => 6

    case Call     => 7
    case CallCode => 7

    case _ => 0
  }

  val stackWriteCount: PartialFunction[EVM.Op, Int] = {
    case Stop                  => 0
    case CallDataCopy          => 0
    case CodeCopy              => 0
    case ExtCodeCopy           => 0
    case Pop                   => 0
    case MStore                => 0
    case MStore8               => 0
    case SStore                => 0
    case Jump                  => 0
    case JumpI                 => 0
    case SelfAddressedJump(_)  => 0
    case SelfAddressedJumpI(_) => 0

    case JumpDest     => 0
    case JumpDest(_)  => 0
    case Log(n)       => 0
    case Return       => 0
    case Invalid      => 0
    case Revert       => 0
    case SelfDestruct => 0

    case Dup(n)  => n + 1
    case Swap(n) => n + 1

    case _ => 1

  }

}
