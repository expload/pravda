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

object JumpTargetRecognizer {

  def apply(ops1: List[(Int, Op)], length: Long): Option[(List[(Int, Op)], Set[WithJumpDest])] = {
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
          val creativeJumps = Emulator.jumps(creativeBlocks)

          val runtimeBlocks = Blocks.split(runtime.map(_._2))
          val runtimeJumps = Emulator.jumps(runtimeBlocks)

          val creativeJumpsMap: Map[Int, AddressedJumpOp] = creativeJumps._1
            .map({
              case j @ JumpI(addr, _) => addr -> j
              case j @ Jump(addr, _)  => addr -> j
            })
            .toMap

          val newCreativeOps = (creative).map({
            case (ind, SelfAddressedJumpI(ind1)) if creativeJumpsMap.contains(ind1) => ind -> creativeJumpsMap(ind1)
            case (ind, SelfAddressedJump(ind1)) if creativeJumpsMap.contains(ind1)  => ind -> creativeJumpsMap(ind1)
            case a                                                                  => a
          })

          val runtimeJumpsMap: Map[Int, AddressedJumpOp] = runtimeJumps._1
            .map({
              case j @ JumpI(addr, _) => addr -> j
              case j @ Jump(addr, _)  => addr -> j
            })
            .toMap

          val newRuntimeOps = (runtime).map({
            case (ind, SelfAddressedJumpI(ind1)) if runtimeJumpsMap.contains(ind1) => ind -> runtimeJumpsMap(ind1)
            case (ind, SelfAddressedJump(ind1)) if runtimeJumpsMap.contains(ind1)  => ind -> runtimeJumpsMap(ind1)
            case a                                                                 => a
          })

          (newCreativeOps ::: newRuntimeOps) -> (creativeJumps._2 ++ runtimeJumps._2)
      })

  }
}
