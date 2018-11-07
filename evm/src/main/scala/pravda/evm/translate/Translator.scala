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
import pravda.evm.abi.parse.ABIParser.{ABIObject}
import pravda.evm.translate.opcode.{FunctionCheckerTranslator, JumpDestinationPrepare, SimpleTranslation}

object Translator {

  type Converted = Either[EVM.Op, List[asm.Operation]]

  val startLabelName = "__start_evm_program"

  def apply(ops: List[EVM.Op], abi: List[ABIObject]): Either[String, List[asm.Operation]] = {
    val (funcs, _, _) = ABIObject.split(abi)
    FunctionCheckerTranslator
      .evmToOps(ops, funcs)
      .map({
        case Left(op)     => SimpleTranslation.evmOpToOps(op)
        case Right(value) => Right(value)
      })
      .map(_.left.map(op => s"incorrect op: ${op.toString}"))
      .sequence
      .map(_.flatten)
  }

  def removeDeployCode(ops: List[(Int, EVM.Op)]): Either[String, List[(Int, EVM.Op)]] = {
    ops
      .takeWhile({
        case (_, CodeCopy) => false
        case _             => true
      })
      .reverse
      .tail
      .headOption match {
      case Some((_, Push(address))) =>
        val offset = BigInt(1, address.toArray).intValue()
        Right(
          ops
            .map({ case (ind, op) => ind - offset -> op })
            .filterNot(_._1 < 0))
      case _ => Left("Parse error")
    }
  }

  def translateActualContract(ops: List[(Int, EVM.Op)], abi: List[ABIObject]): Either[String, List[asm.Operation]] = {
    import JumpDestinationPrepare._

    removeDeployCode(ops).flatMap({ actualContract =>
      val filteredOps = actualContract.map(jumpDestToAddressed)
      val jumpDests = filteredOps.collect({ case j @ JumpDest(x) => j }).zipWithIndex
      val prepare = prepared(jumpDests)
      Translator(filteredOps, abi).map(opcodes => prepare ::: (asm.Operation.Label(startLabelName) :: opcodes))
    })
  }

}
