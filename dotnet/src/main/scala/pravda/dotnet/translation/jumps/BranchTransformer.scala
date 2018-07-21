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

package pravda.dotnet.translation.jumps

import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._

object BranchTransformer {

  def transformBranches(cil: List[CIL.Op], labelPrefix: String): List[CIL.Op] = {
    val offsets = cil
      .foldLeft((0, List.empty[Int])) {
        case ((curOffset, offsets), opcode) =>
          val newOffsets = opcode match {
            case BrS(t) if t != 0 => List(curOffset + t + 2)
            case Br(t) if t != 0  => List(curOffset + t + 5)
            case BrFalseS(t)      => List(curOffset + t + 2)
            case BrFalse(t)       => List(curOffset + t + 5)
            case BrTrueS(t)       => List(curOffset + t + 2)
            case BrTrue(t)        => List(curOffset + t + 5)
            case BltS(t)          => List(curOffset + t + 2)
            case Blt(t)           => List(curOffset + t + 5)
            case BltUnS(t)        => List(curOffset + t + 2)
            case BltUn(t)         => List(curOffset + t + 5)
            case BgtS(t)          => List(curOffset + t + 2)
            case Bgt(t)           => List(curOffset + t + 5)
            case BgtUnS(t)        => List(curOffset + t + 2)
            case BgtUn(t)         => List(curOffset + t + 5)
            case BleS(t)          => List(curOffset + t + 2)
            case Ble(t)           => List(curOffset + t + 5)
            case BleUnS(t)        => List(curOffset + t + 2)
            case BleUn(t)         => List(curOffset + t + 5)
            case BgeS(t)          => List(curOffset + t + 2)
            case Bge(t)           => List(curOffset + t + 5)
            case BgeUnS(t)        => List(curOffset + t + 2)
            case BgeUn(t)         => List(curOffset + t + 5)
            case BeqS(t)          => List(curOffset + t + 2)
            case Beq(t)           => List(curOffset + t + 5)
            case BneUnS(t)        => List(curOffset + t + 2)
            case BneUn(t)         => List(curOffset + t + 5)
            //case Switch(ts)            => ts.filter(_ != 0).map(_ + curOffset + 1)
            case _ => List.empty
          }
          (curOffset + opcode.size, offsets ++ newOffsets)
      }
      ._2

    def mkLabel(i: Int): String = labelPrefix + "_br" + i.toString

    val opcodesWithLabels = cil
      .foldLeft((0, List.empty[CIL.Op])) {
        case ((curOffset, opcodes), opcode) =>
          val newOpcodes = opcode match {
            case BrS(0)      => List(Nop)
            case BrS(t)      => List(Jump(mkLabel(curOffset + t + 2)))
            case Br(t)       => List(Jump(mkLabel(curOffset + t + 5)))
            case BrFalseS(t) => List(Not, JumpI(mkLabel(curOffset + t + 2)))
            case BrFalse(t)  => List(Not, JumpI(mkLabel(curOffset + t + 5)))
            case BrTrueS(t)  => List(JumpI(mkLabel(curOffset + t + 2)))
            case BrTrue(t)   => List(JumpI(mkLabel(curOffset + t + 5)))
            case BltS(t)     => List(Clt, JumpI(mkLabel(curOffset + t + 2)))
            case Blt(t)      => List(Clt, JumpI(mkLabel(curOffset + t + 5)))
            case BltUnS(t)   => List(Clt, JumpI(mkLabel(curOffset + t + 2)))
            case BltUn(t)    => List(Clt, JumpI(mkLabel(curOffset + t + 5)))
            case BgtS(t)     => List(Cgt, JumpI(mkLabel(curOffset + t + 2)))
            case Bgt(t)      => List(Cgt, JumpI(mkLabel(curOffset + t + 5)))
            case BgtUnS(t)   => List(Cgt, JumpI(mkLabel(curOffset + t + 2)))
            case BgtUn(t)    => List(Cgt, JumpI(mkLabel(curOffset + t + 5)))
            case BleS(t)     => List(Cgt, Not, JumpI(mkLabel(curOffset + t + 2)))
            case Ble(t)      => List(Cgt, Not, JumpI(mkLabel(curOffset + t + 5)))
            case BleUnS(t)   => List(Cgt, Not, JumpI(mkLabel(curOffset + t + 2)))
            case BleUn(t)    => List(Cgt, Not, JumpI(mkLabel(curOffset + t + 5)))
            case BgeS(t)     => List(Clt, Not, JumpI(mkLabel(curOffset + t + 2)))
            case Bge(t)      => List(Clt, Not, JumpI(mkLabel(curOffset + t + 5)))
            case BgeUnS(t)   => List(Clt, Not, JumpI(mkLabel(curOffset + t + 2)))
            case BgeUn(t)    => List(Clt, Not, JumpI(mkLabel(curOffset + t + 5)))
            case BeqS(t)     => List(Ceq, JumpI(mkLabel(curOffset + t + 2)))
            case Beq(t)      => List(Ceq, JumpI(mkLabel(curOffset + t + 5)))
            case BneUnS(t)   => List(Ceq, Not, JumpI(mkLabel(curOffset + t + 2)))
            case BneUn(t)    => List(Ceq, Not, JumpI(mkLabel(curOffset + t + 5)))
            //case Switch(ts) => ts.filter(_ != 0).map(t => Label(mkLabel(curOffset + t + 1))) // FIXME switch
            case opcode => List(opcode)
          }
          val newOpcodesWithLabel =
            if (offsets.contains(curOffset)) Label(mkLabel(curOffset)) :: newOpcodes else newOpcodes
          (curOffset + opcode.size, opcodes ++ newOpcodesWithLabel)
      }
      ._2

    opcodesWithLabels
  }
}
