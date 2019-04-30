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

package pravda.dotnet.translation

package jumps

import pravda.dotnet.parser.CIL
import pravda.dotnet.parser.CIL._
import pravda.dotnet.translation.data._
import pravda.dotnet.translation.opcode.{JumpsTranslation, OpcodeTranslator}

/**
  * Algorithm to find stack offsets for labels in the given program.
  * It means we're trying to predict for each label what stack offset will be when the execution is on that label.
  *
  * Algorithm consists of several converging passes through the CIL opcodes.
  * On each pass we have stack offsets for some labels, initially for no labels,
  * and current stack offset that we are calculating through the pass.
  * Current stack offset can be __determined__ or __undetermined__ in some situations.
  * We start with determined stack offset 0 on the first opcode.
  * For opcodes that are not Jump, JumpI or Label we know how it will change the stack offset,
  * e.g. we know some value `delta` and after this opcode `stack offset` will be `stack offset` + `delta`.
  * Thus we can easily calculate stack offset after any sequence of such opcodes, simply summing their `delta`s.
  *
  * When we encounter one of `Jump`, `JumpI`, `Label` opcodes we have several situations:
  *  - `Jump` or `JumpI` on label with known stack offset and determined stack offset through the current pass.
  *    In this situation we check that these two values are equal or throw an error.
  *
  *  - `Jump` on label with __unknown__ stack offset and determined stack offset.
  *    We update known stack offsets for this label and make stack offset undetermined,
  *    because we jumped to other code location and don't know what stack offset can be for next opcodes.
  *
  *  - `JumpI` on label with __unknown__ stack offset and determined stack offset.
  *    We update known stack offsets for this label.
  *    The stack offset is still determined, because we may not jump anywhere on false condition.
  *
  *  - `Label` on label with known stack offset and determined stack offset.
  *    We check that these two values are equal or throw an error
  *  - `Label` on label with __unknown__ stack offset and determined stack offset.
  *    We update known stack offsets for this label.
  *
  *  - `Label` on label with known stack offset and __undetermined__ stack offset.
  *    We make current stack offset determined with value equal to stack offset on this label.
  *
  *  - In other situations we don't do anything to known stack offsets for labels and current stack offset.
  *
  * It can be proven that doing such passes while at least one stack offset for some label changes on each pass
  * leads to correct mapping of stack offsets to the labels or error if such mapping is not possible at all.
  */
object StackOffsetResolver {

  /**
    * Performs one step in converging pass
    *
    * @param op current opcode
    * @param labelOffsets already found stack offsets for labeles
    * @param stackOffsetO current stack offset if can be determined
    * @return error or new stack offsets for labels and new current stack offset
    */
  def transformStackOffset(
      op: CIL.Op,
      labelOffsets: Map[String, Int],
      stackOffsetO: Option[Int]): Either[InnerTranslationError, (Map[String, Int], Option[Int])] = {

    val unstableStackError =
      Left(InternalError("Unsupported sequence of instructions: stack is unstable"))

    op match {
      case Label(label) =>
        (labelOffsets.get(label), stackOffsetO) match {
          case (Some(offset), Some(stackOffset)) if offset != stackOffset + JumpsTranslation.labelStackOffset =>
            unstableStackError
          case (Some(offset), None)      => Right((labelOffsets, Some(offset)))
          case (None, Some(stackOffset)) => Right((labelOffsets.updated(label, stackOffset), Some(stackOffset)))
          case (_, offset @ _)           => Right((labelOffsets, offset))
        }
      case Jump(label) =>
        (labelOffsets.get(label), stackOffsetO) match {
          case (Some(offset), Some(stackOffset)) if offset != stackOffset + JumpsTranslation.jumpStackOffset =>
            unstableStackError
          case (None, Some(stackOffset)) =>
            Right((labelOffsets.updated(label, stackOffset + JumpsTranslation.jumpStackOffset), None))
          case (_, offset @ _) => Right((labelOffsets, None))
        }
      case JumpI(label) =>
        (labelOffsets.get(label), stackOffsetO) match {
          case (Some(offset), Some(stackOffset)) if offset != stackOffset + JumpsTranslation.jumpIStackOffset =>
            unstableStackError
          case (None, Some(stackOffset)) =>
            Right((labelOffsets.updated(label, stackOffset + JumpsTranslation.jumpIStackOffset), Some(stackOffset)))
          case (_, offset @ _) => Right((labelOffsets, offset))
        }
      case Ret   => Right((labelOffsets, None))
      case other => Right((labelOffsets, stackOffsetO))
    }
  }

  /**
    * Perform passes until mapping of stack offsets to labels doesn't converge
    *
    * @param cil raw CIL opcodes
    * @param ctx info about method that contains given CIL opcodes
    * @return error or stack offsets for labels in the `cil`
    */
  def convergeLabelOffsets(cil: List[CIL.Op], ctx: MethodTranslationCtx): Either[TranslationError, Map[String, Int]] = {
    var errorE: Either[InnerTranslationError, Unit] = Right(())
    var cilOffset = 0
    var lastOffsets = Map.empty[String, Int]
    var offsets = Map.empty[String, Int]

    do {
      var curCil = cil
      var stackOffsetO: Option[Int] = Some(0)
      cilOffset = 0
      lastOffsets = offsets

      while (curCil.nonEmpty && errorE.isRight) {
        errorE = for {
          so <- transformStackOffset(curCil.head, offsets, stackOffsetO)
          (newLabelOffsets, newStackOffsetO) = so
          offsetRes <- OpcodeTranslator.deltaOffset(curCil, ctx)
          (taken, deltaOffset) = offsetRes
        } yield {
          val (takenCil, restCil) = curCil.splitAt(taken)
          curCil = restCil
          stackOffsetO = newStackOffsetO.map(_ + deltaOffset)
          cilOffset += takenCil.map(_.size).sum
          offsets = newLabelOffsets
        }
      }
    } while (errorE.isRight && offsets != lastOffsets)

    errorE match {
      case Left(err) => Left(TranslationError(err, ctx.debugInfo.flatMap(DebugInfo.firstSourceMark(_, cilOffset))))
      case Right(()) => Right(offsets)
    }
  }
}
