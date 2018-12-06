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
import pravda.evm.EVM._
import pravda.evm.abi.parse.AbiParser.AbiFunction
import pravda.evm.translate.Translator.Converted
import pravda.vm.Opcodes

import scala.annotation.tailrec

object FunctionSelectorTranslator {

  def evmToOps(ops: List[EVM.Op], abi: List[AbiFunction]): List[Converted] = {
    val destinations = ops
      .dropWhile {
        case CallDataSize => false
        case _            => true
      }
      .takeWhile {
        case JumpDest(_) => false
        case _           => true
      }

    case class FunctionDestination(function: AbiFunction, address: Array[Byte])

    @tailrec def jumps(ops: List[EVM.Op],
                       lastFunction: Option[AbiFunction],
                       acc: List[FunctionDestination]): List[FunctionDestination] =
      ops match {
        case Push(address) :: JumpI :: xs =>
          val addr = address.toArray

          lastFunction match {
            case Some(f) => jumps(xs, lastFunction, FunctionDestination(f, addr) :: acc)
            case None    => jumps(xs, lastFunction, acc)
          }
        case Push(addr) :: xs =>
          val address = addr.toArray.toList
          abi.find { case f @ AbiFunction(_, _, _, _, _, _, _) => f.id == address } match {
            case Some(f) => jumps(xs, Some(f), acc)
            case None    => jumps(xs, lastFunction, acc)
          }
        case x :: xs =>
          jumps(xs, lastFunction, acc)
        case Nil => acc
      }

    destinations match {
      case Nil => ops.map(Left(_))
      case _ =>
        val newJumps = jumps(destinations, None, List.empty).flatMap(
          f =>
            codeToOps(Opcodes.DUP) ::: pushString(f.function.newName.getOrElse(f.function.name)) ::
              codeToOps(Opcodes.EQ) ::: pushBigInt(BigInt(1, f.address)) :: jumpi)

        val l1 = ops
          .takeWhile {
            case CallDataSize => false
            case _            => true
          }
          .init
          .map(Left(_))
        val l2: List[Converted] =
          (newJumps ++ (pushString("incorrect function name") :: codeToOps(Opcodes.THROW))).map(op => Right(List(op)))
        val l3 = ops
          .dropWhile {
            case JumpDest(_) => false
            case _           => true
          }
          .map(Left(_))
        l1 ++ l2 ++ l3
    }
  }
}
