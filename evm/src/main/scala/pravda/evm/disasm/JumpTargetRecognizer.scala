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

import pravda.evm.EVM._
import pravda.evm.disasm.Blocks.WithJumpDest
import pravda.evm.translate.Translator._

object JumpTargetRecognizer {

  def apply(ops: EvmCode): Either[Set[WithJumpDest], List[Addressed[Op]]] = {

    val blocks = Blocks.split(ops.code.map(_._2))
    val (jumps, jumpdests) = Emulator.jumps(blocks)

    val jumpsMap: Map[Int, AddressedJumpOp] = jumps.map {
      case j @ JumpI(addr, _) => addr -> j
      case j @ Jump(addr, _)  => addr -> j
    }.toMap

    val newOps = ops.code.map {
      case (ind, SelfAddressedJumpI(ind1)) if jumpsMap.contains(ind1) => ind -> jumpsMap(ind1)
      case (ind, SelfAddressedJump(ind1)) if jumpsMap.contains(ind1)  => ind -> jumpsMap(ind1)
      case a                                                          => a
    }

    if (jumpdests.isEmpty)
      Right(newOps)
    else Left(jumpdests)
  }
}
