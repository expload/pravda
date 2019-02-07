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
import pravda.evm.EVM.JumpDest
import pravda.evm.translate.Translator.startLabelName
import pravda.vm.asm.Operation
import pravda.vm.{Opcodes, asm}
import pravda.vm.asm.Operation.PushOffset

object JumpDestinationPrepare {

  def jumpDestToOps(op: (EVM.JumpDest, Int)): List[asm.Operation] =
    op match {
      case (JumpDest(addr), ind) =>
        List(
          asm.Operation.Label(nameByNumber(ind)),
          asm.Operation(Opcodes.DUP),
          pushBigInt(addr),
          asm.Operation(Opcodes.EQ),
          asm.Operation(Opcodes.NOT),
          Operation.JumpI(Some(nameByNumber(ind + 1))),
          asm.Operation(Opcodes.POP),
          PushOffset(nameByAddress(addr)),
          Operation.Jump(None)
        )
      case _ => List()
    }

  def lastBranch(n: Int): List[asm.Operation] =
    if (n > 0)
      List(asm.Operation.Label(nameByNumber(n)), pushString("Incorrect destination"), asm.Operation(Opcodes.THROW))
    else Nil

  def jumpDestToAddressed(indexed: (Int, EVM.Op)): EVM.Op = indexed match {
    case (ind, JumpDest) => JumpDest(ind)
    case (_, op)         => op
  }

  def prepared(jumpDests: List[(JumpDest, Int)]): List[asm.Operation] =
    Operation.Jump(Some(startLabelName)) :: jumpDests.flatMap(jumpDestToOps) ::: lastBranch(jumpDests.size)
}
