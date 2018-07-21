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

package pravda.dotnet.translation.opcode

import pravda.dotnet.parsers.CIL
import pravda.vm.asm
import pravda.dotnet.translation.data._
import pravda.vm.asm.Operation._
import pravda.vm.Opcodes

import scala.annotation.tailrec

// We can't compute delta offset from generated asm code,
// because in general asm generation requires stack offset,
// which itself requires delta offsets
trait OpcodeTranslator {

  def additionalFunctions(ops: List[CIL.Op], ctx: MethodTranslationCtx)
    : Either[TranslationError, (OpcodeTranslator.Taken, List[OpcodeTranslator.AdditionalFunction])] =
    Left(UnknownOpcode)

  def deltaOffset(ops: List[CIL.Op], ctx: MethodTranslationCtx): Either[TranslationError, (OpcodeTranslator.Taken, Int)]

  def asmOps(ops: List[CIL.Op],
             stackOffsetO: Option[Int],
             ctx: MethodTranslationCtx): Either[TranslationError, (OpcodeTranslator.Taken, List[asm.Operation])]
}

object OpcodeTranslatorOnlyAsm {

  def asmOpOffset(asmOp: asm.Operation): Int = asmOp match {
    case Nop            => 0
    case Comment(_)     => 0
    case Label(_)       => 0
    case Meta(_)        => 0
    case Push(_)        => 1
    case New(_)         => 1
    case Jump(name)     => if (name.isDefined) 0 else -1
    case JumpI(name)    => if (name.isDefined) -1 else -2
    case Call(name)     => if (name.isDefined) 0 else -1
    case StructMut(key) => if (key.isDefined) -2 else -3
    case StructGet(key) => if (key.isDefined) 0 else -1
    case Orphan(opcode) =>
      opcode match {
        case Opcodes.STOP => 0
        case Opcodes.RET  => 0

        case Opcodes.PCALL => 0
        case Opcodes.LCALL => 0

        case Opcodes.POP   => -1
        case Opcodes.DUP   => 1
        case Opcodes.DUPN  => 0
        case Opcodes.SWAP  => 0
        case Opcodes.SWAPN => -1

        case Opcodes.ARRAY_GET         => -1
        case Opcodes.STRUCT_GET_STATIC => 0
        case Opcodes.ARRAY_MUT         => -3
        case Opcodes.STRUCT_MUT_STATIC => -2
        case Opcodes.PRIMITIVE_PUT     => 0
        case Opcodes.PRIMITIVE_GET     => 0
        case Opcodes.NEW_ARRAY         => -1
        case Opcodes.LENGTH            => 0
        case Opcodes.BALANCE           => 0
        case Opcodes.OWNER             => 0

        case Opcodes.SPUT   => -2
        case Opcodes.SGET   => 0
        case Opcodes.SDROP  => -1
        case Opcodes.SEXIST => 0

        case Opcodes.ADD => -1
        case Opcodes.MUL => -1
        case Opcodes.DIV => -1
        case Opcodes.MOD => -1
        case Opcodes.LT  => -1
        case Opcodes.GT  => -1
        case Opcodes.NOT => 0
        case Opcodes.AND => -1
        case Opcodes.OR  => -1
        case Opcodes.XOR => -1
        case Opcodes.EQ  => -1

        case Opcodes.CAST   => -1
        case Opcodes.CONCAT => -1
        case Opcodes.SLICE  => -2

        case Opcodes.FROM => 1

        case Opcodes.META => 0

        case Opcodes.PADDR   => 1
        case Opcodes.PCREATE => 0
        case Opcodes.PUPDATE => -2

        case Opcodes.TRANSFER  => -2
        case Opcodes.PTRANSFER => -2
      }
  }
}

trait OpcodeTranslatorOnlyAsm extends OpcodeTranslator {

  def deltaOffset(ops: List[CIL.Op],
                  ctx: MethodTranslationCtx): Either[TranslationError, (OpcodeTranslator.Taken, Int)] =
    asmOps(ops, None, ctx).map { case (rest, aOps) => (rest, aOps.map(OpcodeTranslatorOnlyAsm.asmOpOffset).sum) }
}

trait OneToManyTranslator extends OpcodeTranslator {

  def additionalFunctionsOne(
      op: CIL.Op,
      ctx: MethodTranslationCtx): Either[TranslationError, List[OpcodeTranslator.AdditionalFunction]] =
    Left(UnknownOpcode)

  def deltaOffsetOne(op: CIL.Op, ctx: MethodTranslationCtx): Either[TranslationError, Int]

  def asmOpsOne(op: CIL.Op,
                stackOffsetO: Option[Int],
                ctx: MethodTranslationCtx): Either[TranslationError, List[asm.Operation]]

  override def additionalFunctions(ops: List[CIL.Op], ctx: MethodTranslationCtx)
    : Either[TranslationError, (OpcodeTranslator.Taken, List[OpcodeTranslator.AdditionalFunction])] =
    ops match {
      case head :: _ => additionalFunctionsOne(head, ctx).map((1, _))
      case _         => Right((0, List.empty))
    }

  def deltaOffset(ops: List[CIL.Op],
                  ctx: MethodTranslationCtx): Either[TranslationError, (OpcodeTranslator.Taken, Int)] =
    ops match {
      case head :: _ => deltaOffsetOne(head, ctx).map((1, _))
      case _         => Right((0, 0))
    }

  def asmOps(ops: List[CIL.Op],
             stackOffsetO: Option[Int],
             ctx: MethodTranslationCtx): Either[TranslationError, (OpcodeTranslator.Taken, List[asm.Operation])] =
    ops match {
      case head :: _ => asmOpsOne(head, stackOffsetO, ctx).map((1, _))
      case _         => Right((1, List.empty))
    }
}

trait OneToManyTranslatorOnlyAsm extends OneToManyTranslator {

  def deltaOffsetOne(op: CIL.Op, ctx: MethodTranslationCtx): Either[TranslationError, Int] =
    asmOpsOne(op, None, ctx).map(_.map(OpcodeTranslatorOnlyAsm.asmOpOffset).sum)
}

object OpcodeTranslator {

  type Taken = Int

  final case class AdditionalFunction(name: String, ops: List[asm.Operation])

  val translators: List[OpcodeTranslator] =
    List(
      ArrayInitializationTranslation,
      MappingInitializationTranslation,
      ConvertTranslation,
      SimpleTranslations,
      ArgsLocalsTranslations,
      FieldsTranslation,
      JumpsTranslation,
      StringTranslation,
      BytesTranslation,
      ArrayTranslation,
      CallsTransation
    )

  private def notUnknownOpcode[T](res: Either[TranslationError, T]): Boolean = res match {
    case Left(UnknownOpcode) => false
    case _                   => true
  }

  @tailrec
  private def findAndReturn[A, B](l: List[A], f: A => B, pred: B => Boolean): Option[B] = l match {
    case head :: tail =>
      val fHead = f(head)
      if (pred(fHead)) {
        Some(fHead)
      } else {
        findAndReturn(tail, f, pred)
      }
    case _ => None
  }

  def additionalFunctions(ops: List[CIL.Op], ctx: MethodTranslationCtx): (Taken, List[AdditionalFunction]) =
    findAndReturn(translators, (t: OpcodeTranslator) => t.additionalFunctions(ops, ctx), notUnknownOpcode)
      .flatMap(_.toOption)
      .getOrElse((0, List.empty))

  def asmOps(ops: List[CIL.Op],
             stackOffsetO: Option[Int],
             ctx: MethodTranslationCtx): Either[TranslationError, (Taken, List[asm.Operation])] =
    findAndReturn(translators, (t: OpcodeTranslator) => t.asmOps(ops, stackOffsetO, ctx), notUnknownOpcode)
      .getOrElse(Left(NotSupportedOpcode(ops.head)))

  def deltaOffset(ops: List[CIL.Op], ctx: MethodTranslationCtx): Either[TranslationError, (Taken, Int)] =
    findAndReturn(translators, (t: OpcodeTranslator) => t.deltaOffset(ops, ctx), notUnknownOpcode)
      .getOrElse(Left(NotSupportedOpcode(ops.head)))
}
