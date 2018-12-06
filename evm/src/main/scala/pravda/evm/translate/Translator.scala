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

package pravda.evm.translate

import pravda.evm.EVM._
import pravda.vm.asm
import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import pravda.evm.EVM
import pravda.evm.abi.parse.ABIParser.ABIObject
import pravda.evm.translate.opcode.{FunctionSelectorTranslator, JumpDestinationPrepare, SimpleTranslation}

object Translator {

  trait EvmCode

  case class CreationCode(code: List[Addressed[EVM.Op]]) extends EvmCode
  case class ActualCode(code: List[Addressed[EVM.Op]])   extends EvmCode

  type Converted = Either[EVM.Op, List[asm.Operation]]
  type Addressed[T] = (Int, T)
  type ContractCode = (CreationCode, ActualCode)

  val startLabelName = "__start_evm_program"

  def apply(ops: List[EVM.Op], abi: List[ABIObject]): Either[String, List[asm.Operation]] = {
    val (funcs, _, _) = ABIObject.unzip(abi)
    FunctionSelectorTranslator
      .evmToOps(ops, funcs)
      .map({
        case Left(op)     => SimpleTranslation.evmOpToOps(op)
        case Right(value) => Right(value)
      })
      .map(_.left.map(op => s"incorrect op: ${op.toString}"))
      .sequence
      .map(_.flatten)
  }

  def split(ops: List[Addressed[EVM.Op]]): Either[String, ContractCode] = {
    ???
    //JumpTargetRecognizer(ops)
  }

  def translateActualContract(ops: List[Addressed[EVM.Op]],
                              abi: List[ABIObject]): Either[String, List[asm.Operation]] = {
    import JumpDestinationPrepare._

    split(ops).flatMap({
      case (creationCode, actualContract) =>
        val filteredOps = actualContract.code.map(jumpDestToAddressed)
        val jumpDests = filteredOps.collect({ case j @ JumpDest(x) => j }).zipWithIndex
        val prepare = prepared(jumpDests)
        Translator(filteredOps, abi).map(opcodes => prepare ::: (asm.Operation.Label(startLabelName) :: opcodes))
    })
  }

}
