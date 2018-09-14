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

import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.translation.data._
import pravda.dotnet.translation.opcode.{JumpsTranslation, OpcodeTranslator}

object StackOffsetResolver {

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
            println(label)
            unstableStackError
          case (Some(offset), None)      => Right((labelOffsets, Some(offset)))
          case (None, Some(stackOffset)) => Right((labelOffsets.updated(label, stackOffset), Some(stackOffset)))
          case (_, offset @ _)           => Right((labelOffsets, offset))
        }
      case Jump(label) =>
        (labelOffsets.get(label), stackOffsetO) match {
          case (Some(offset), Some(stackOffset)) if offset != stackOffset + JumpsTranslation.jumpStackOffset =>
            println(label)
            unstableStackError
          case (None, Some(stackOffset)) =>
            Right((labelOffsets.updated(label, stackOffset + JumpsTranslation.jumpStackOffset), None))
          case (_, offset @ _) => Right((labelOffsets, None))
        }
      case JumpI(label) =>
        (labelOffsets.get(label), stackOffsetO) match {
          case (Some(offset), Some(stackOffset)) if offset != stackOffset + JumpsTranslation.jumpIStackOffset =>
            println(label)
            unstableStackError
          case (None, Some(stackOffset)) =>
            Right((labelOffsets.updated(label, stackOffset + JumpsTranslation.jumpIStackOffset), Some(stackOffset)))
          case (_, offset @ _) => Right((labelOffsets, offset))
        }
      case Ret   => Right((labelOffsets, None))
      case other => Right((labelOffsets, stackOffsetO))
    }
  }

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
