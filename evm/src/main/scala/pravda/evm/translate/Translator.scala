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

import pravda.vm.{Data, Opcodes, asm}
import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import pravda.evm.EVM
import pravda.evm.abi.parse.AbiParser.AbiObject
import pravda.evm.disasm.{Blocks, JumpTargetRecognizer, StackSizePredictor}
import pravda.evm.translate.opcode._
import pravda.vm.asm.Operation

object Translator {

  trait EvmCode {
    def code: List[Addressed[EVM.Op]]
  }

  case class CreationCode(code: List[Addressed[EVM.Op]]) extends EvmCode
  case class ActualCode(code: List[Addressed[EVM.Op]])   extends EvmCode
  case class Code(code: List[Addressed[EVM.Op]])         extends EvmCode

  type Converted = Either[EVM.Op, List[asm.Operation]]
  type Addressed[T] = (Int, T)
  type ContractCode = (CreationCode, ActualCode)

  val startLabelName = "__start_evm_program"
  val defaultMemorySize = 2000

  def apply(ops: List[EVM.Op], abi: List[AbiObject]): Either[String, List[asm.Operation]] = {
    val (funcs, _, _) = AbiObject.unwrap(abi)
    FunctionSelectorTranslator
      .evmToOps(ops, funcs)
      .map {
        case Left(op)     => SimpleTranslation.evmOpToOps(op)
        case Right(value) => Right(value)
      }
      .map(_.left.map(op => s"incorrect op: ${op.toString}"))
      .sequence
      .map(_.flatten)
  }

  def translateActualContract(ops: List[Addressed[EVM.Op]],
                              abi: List[AbiObject]): Either[String, List[asm.Operation]] = {

    for {
      code <- Blocks.splitToCreativeAndRuntime(ops)
      code <- JumpTargetRecognizer(code._2).left.map(_.toString)
      ops1 = StackSizePredictor.clear(StackSizePredictor.emulate(code.map(_._2)))
      res <- Translator(ops1, abi).map(opcodes =>
        asm.Operation
          .Label(startLabelName) :: createArray(defaultMemorySize) ::: opcodes ::: StdlibAsm.readWordFunction.code ::: StdlibAsm.writeWordFunction.code)
    } yield res
  }

  def createArray(size: Int): List[Operation] =
    List(
      pushInt(size),
      pushType(Data.Type.Int8),
      Operation(Opcodes.NEW_ARRAY)
    )
}
