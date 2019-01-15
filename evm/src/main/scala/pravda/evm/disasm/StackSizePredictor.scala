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

import scala.annotation.tailrec

object StackSizePredictor {

  def clear(ops: List[(Op, Int)]): List[Op] =
    ops.map {
      case (MLoad, ind)        => MLoad(ind)
      case (MStore, ind)       => MStore(ind)
      case (MStore8, ind)      => MStore8(ind)
      case (CallDataSize, ind) => CallDataSize(ind)
      case (CallDataLoad, ind) => CallDataLoad(ind)
      case (Sha3, ind)         => Sha3(ind)
      case (op, _)             => op
    }

  def emulate(ops: List[Op]): List[(Op, Int)] = {
    val ops1 = ops.map(_ -> -1).toArray
    import OpCodes._
    type ToJumpDest = Option[Int]
    type StackSize = Int
    type ToContinue = Option[Int]

    @tailrec def emulate(ind: Int, size: Int): (ToJumpDest, ToContinue, StackSize) = {
      ops1(ind) match {
        case (x, s) if terminate(x) =>
          ops1(ind) = (x, size)
          (None, None, handle(x, size))
        case (j @ Jump(addr, dest), s) =>
          ops1(ind) = (j, size)
          (Some(dest), None, handle(j, size))
        case (j @ JumpI(addr, dest), s) =>
          ops1(ind) = (j, size)
          (Some(dest), Some(ind + 1), handle(j, size))
        case (x, s) =>
          ops1(ind) = (x, size)
          emulate(ind + 1, handle(x, size))
      }
    }

    def emulateS(to: Int, size: Int): Unit = {
      to match {
        case ind if ind >= 0 =>
          val (to, cont, s) = emulate(ind, size)
          to match {
            case Some(addr) =>
              emulateS(ops1.indexWhere {
                case (JumpDest(`addr`), i) if i < 0 => true
                case _                              => false
              }, s)
            case _ =>
          }

          cont match {
            case Some(addr) if ops1(addr)._2 < 0 => emulateS(addr, s)
            case _                               =>
          }
        case _ =>
      }
    }

    emulateS(0, 0)
    ops1.toList
  }
}
