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

import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import pravda.evm.EVM
import pravda.evm.EVM._
import pravda.evm.abi.parse.AbiParser.AbiObject
import pravda.evm.disasm.{JumpTargetRecognizer, StackSizePredictor}
import pravda.evm.translate.opcode._
import pravda.vm.asm.Operation
import pravda.vm.{Data, Opcodes, asm}

object Translator {

  trait EvmCode

  case class CreationCode(code: List[Addressed[EVM.Op]]) extends EvmCode
  case class ActualCode(code: List[Addressed[EVM.Op]])   extends EvmCode

  type Converted = Either[EVM.Op, List[asm.Operation]]
  type Addressed[T] = (Int, T)
  type ContractCode = (CreationCode, ActualCode)

  val startLabelName = "__start_evm_program"
  val defaultMemorySize = 1024

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

  def split(ops: List[Addressed[EVM.Op]]): Either[String, ContractCode] =
    JumpTargetRecognizer(ops).left.map(_.toString)

  def filterCode(ops: List[EVM.Op]): List[EVM.Op] = {
    import fastparse.byte.all.Bytes

    ops match {
      case Push(Bytes(-128)) ::
            Push(Bytes(0x40)) ::
            MStore(_) ::
            rest =>
        filterCode(rest)
      case Push(Bytes(0x04)) ::
            CallDataSize(1) ::
            Lt ::
            Push(Bytes(_)) ::
            JumpI(_, _)
            :: rest =>
        filterCode(rest)
      case Push(Bytes(0x00)) ::
            CallDataLoad(1) ::
            Push(bs1: Bytes) ::
            Swap(1) ::
            Div ::
            Push(bs2: Bytes) ::
            And ::
            rest
          if bs1 == Bytes.fromHex("0x0100000000000000000000000000000000000000000000000000000000").get &&
            bs2 == Bytes.fromHex("0xffffffff").get =>
        filterCode(rest)
      case CallValue ::
            Dup(1) ::
            IsZero ::
            Push(Bytes(_)) ::
            JumpI(_, _) ::
            Push(Bytes(0x00)) ::
            Dup(1) ::
            Revert :: rest =>
        filterCode(rest)

      case h :: t => h :: filterCode(t)
      case _      => List.empty
    }
  }

  def translateActualContract(ops: List[Addressed[EVM.Op]],
                              abi: List[AbiObject]): Either[String, List[asm.Operation]] = {

    split(ops).flatMap {
      case (creationCode, actualContract) =>
        val filteredActualOps = actualContract.code.map(_._2)
        val ops = StackSizePredictor.clear(StackSizePredictor.emulate(filteredActualOps))
        //println(ops.mkString("\n"))
        val filtered = filterCode(ops)
        //println("---")
        //println(filtered.mkString("\n"))

        Translator(filtered, abi).map(
          opcodes =>
            Operation.Label(startLabelName) ::
              createArray(defaultMemorySize) :::
              Operation(Opcodes.SWAP) ::
            opcodes
        )
    }
  }

  private def createArray(size: Int): List[Operation] =
    List(
      pushInt(size),
      pushType(Data.Type.Int8),
      Operation(Opcodes.NEW_ARRAY)
    )
}
