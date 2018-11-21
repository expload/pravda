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

object Disasm {

  def apply(ops1: List[(Int, Op)], length: Long): Option[List[(Int, Op)]] = {
    val ops = ops1.map({
      case (ind, JumpI) => ind -> SelfAddressedJumpI(ind)
      case (ind, Jump)  => ind -> SelfAddressedJump(ind)
      case a            => a
    })

    Blocks
      .splitToCreativeAndRuntime(ops, length)
      .map({
        case (creative, runtime) =>
          val creativeBlocks = Blocks.split(creative.map(_._2))
          val creativeJumps = Emulator.getJumps(creativeBlocks)

          val runtimeBlocks = Blocks.split(runtime.map(_._2))
          val runtimeJumps = Emulator.getJumps(runtimeBlocks)

          val jumps: Map[Int, AddressedJumpOp] = (creativeJumps ++ runtimeJumps)
            .map({
              case j @ JumpI(addr, _) => addr -> j
              case j @ Jump(addr, _)  => addr -> j
            })
            .toMap

          ops.map({
            case (ind, SelfAddressedJumpI(ind1)) => ind -> jumps(ind1)
            case (ind, SelfAddressedJump(ind1))  => ind -> jumps(ind1)
            case a                               => a
          })
      })

  }
}
