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
import pravda.vm.{Data, Opcodes}
import pravda.vm.asm.Operation

object FunctionSelectorTranslator {

  private def createCallData(argsNum: Int): List[Operation] =
    (argsNum + 2).to(3, -1).toList.flatMap(i => List(pushInt(i), Operation(Opcodes.SWAPN))) ++
      List(Operation(Opcodes.SWAP)) ++
      (argsNum + 1).to(2, -1).toList.flatMap(i => List(pushInt(i), Operation(Opcodes.SWAPN))) ++
      List(pushBytes(Array())) ++
      (1 to argsNum)
        .flatMap(
          i =>
            List(Operation(Opcodes.SWAP)) :::
              cast(Data.Type.Bytes) :::
              List(pushInt(9), Operation(Opcodes.SCALL), Operation(Opcodes.CONCAT))
        )
        .toList ++
      List(pushBytes(Array(0x0, 0x0, 0x0, 0x0)), Operation(Opcodes.CONCAT)) ++
      List(pushInt(3), Operation(Opcodes.SWAPN))

  def evmToOps(ops: List[EVM.Op], abi: List[AbiFunction]): List[Converted] = {

    def aux(code: List[EVM.Op]): List[Converted] = {
      import fastparse.byte.all.Bytes

      code match {
        case (firstOp @ Dup(1)) ::
              Push(Bytes(hash @ _*)) ::
              Eq ::
              Push(_: Bytes) ::
              JumpI(_, addr) :: rest =>
          abi.find(_.id == hash) match {
            case Some(f) =>
              Right(
                codeToOps(Opcodes.DUP) ++
                  List(pushString(f.newName.getOrElse(f.name))) ++
                  codeToOps(Opcodes.EQ, Opcodes.NOT) ++
                  List(Operation.JumpI(Some(s"not_${f.name}"))) ++
                  createCallData(f.inputs.length) ++
                  List(Operation.Jump(Some(nameByAddress(addr))), Operation.Label(s"not_${f.name}"))) :: aux(rest)
            case None => Left(firstOp) :: aux(code.tail)
          }
        case h :: t => Left(h) :: aux(t)
        case _      => List.empty
      }
    }

    aux(ops)
  }
}
