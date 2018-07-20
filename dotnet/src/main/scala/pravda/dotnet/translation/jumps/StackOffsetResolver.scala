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

package pravda.dotnet.translation.jumps

import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.translation.data._
import pravda.dotnet.translation.opcode.{JumpsTranslation, OpcodeTranslator}

object StackOffsetResolver {

  def transformStackOffset(op: CIL.Op,
                           labelOffsets: Map[String, Int],
                           stackOffsetO: Option[Int]): Either[TranslationError, (Map[String, Int], Option[Int])] = {

    val unstableStackError = {
      Left(InternalError("Unsupported sequence of instructions: stack is unstable"))
    }

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
      case other => Right((labelOffsets, stackOffsetO))
    }
  }

  private def traverseLabelOffsets(opcodes: List[CIL.Op],
                                   initLabelOffsets: Map[String, Int],
                                   ctx: MethodTranslationCtx): Either[TranslationError, Map[String, Int]] = {

    def doTraverse(opcodes: List[CIL.Op],
                   offsets: Map[String, Int],
                   stackOffsetO: Option[Int]): Either[TranslationError, Map[String, Int]] =
      opcodes match {
        case op :: _ =>
          for {
            so <- transformStackOffset(op, offsets, stackOffsetO)
            (newLabelOffsets, newStackOffsetO) = so
            offsetRes <- OpcodeTranslator.deltaOffset(opcodes, ctx)
            (taken, deltaOffset) = offsetRes
            newOffsets <- doTraverse(opcodes.drop(taken), newLabelOffsets, newStackOffsetO.map(_ + deltaOffset))
          } yield newOffsets
        case _ => Right(offsets)
      }

    doTraverse(opcodes, initLabelOffsets, Some(0))
  }

  def convergeLabelOffsets(opcodes: List[CIL.Op],
                           ctx: MethodTranslationCtx): Either[TranslationError, Map[String, Int]] = {
    def go(labelOffsets: Map[String, Int]): Either[TranslationError, Map[String, Int]] = {
      for {
        newOffsets <- traverseLabelOffsets(opcodes, labelOffsets, ctx)
        nextGo <- if (newOffsets != labelOffsets) go(newOffsets) else Right(newOffsets)
      } yield nextGo
    }

    go(Map.empty)
  }
}
