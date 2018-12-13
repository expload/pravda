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

import com.google.protobuf.ByteString
import pravda.evm.EVM
import pravda.evm.EVM._
import pravda.evm.abi.parse.AbiParser.AbiFunction
import pravda.evm.translate.Translator.Converted
import pravda.vm.{Data, Opcodes}
import pravda.vm.asm.Operation

import scala.annotation.tailrec

object FunctionSelectorTranslator {

  private def createCallData(argsNum: Int): List[Operation] =
    (argsNum + 1).to(2, -1).toList.flatMap(i => List(pushInt(i), Operation(Opcodes.SWAPN))) ++
      argsNum.to(1, -1).toList.flatMap(i => List(pushInt(i), Operation(Opcodes.SWAPN))) ++
      List(Operation.Push(Data.Primitive.Bytes(ByteString.EMPTY))) ++
      (1 to argsNum)
        .flatMap(
          i =>
            List(Operation(Opcodes.SWAP)) :::
              cast(Data.Type.Bytes) :::
              List(pushInt(9), Operation(Opcodes.SCALL), Operation(Opcodes.CONCAT))
        )
        .toList ++
      List(pushInt(2), Operation(Opcodes.SWAPN), Operation(Opcodes.SWAP))

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

    case class FunctionDestination(function: AbiFunction, address: Int)

    @tailrec def jumps(ops: List[EVM.Op],
                       lastFunction: Option[AbiFunction],
                       acc: List[FunctionDestination]): List[FunctionDestination] =
      ops match {
        case Push(_) :: JumpI(_, dest) :: xs =>
          lastFunction match {
            case Some(f) => jumps(xs, lastFunction, FunctionDestination(f, dest) :: acc)
            case None    => jumps(xs, lastFunction, acc)
          }
        case Push(addr) :: xs =>
          val address = addr.toArray.toList
          abi.find(_.id == address) match {
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
            codeToOps(Opcodes.DUP) ++
              List(pushString(f.function.newName.getOrElse(f.function.name))) ++
              codeToOps(Opcodes.EQ, Opcodes.NOT) ++
              List(Operation.JumpI(Some(s"not_${f.function.name}"))) ++
              createCallData(f.function.inputs.length) ++
              List(Operation.Jump(Some(nameByAddress(f.address))), Operation.Label(s"not_${f.function.name}"))
        )

        val l1 = ops
          .takeWhile {
            case CallDataSize => false
            case _            => true
          }
          .init
          .map(Left(_))
        val l2: List[Converted] =
          (codeToOps(Opcodes.SWAP) ++ newJumps ++ (pushString("incorrect function name") :: codeToOps(Opcodes.THROW))).map(op => Right(List(op)))
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
