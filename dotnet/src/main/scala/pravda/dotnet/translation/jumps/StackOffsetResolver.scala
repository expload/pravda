package pravda.dotnet.translation.jumps

import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.translation.data._
import pravda.dotnet.translation.opcode.{JumpsTranslation, OpcodeTranslator}

object StackOffsetResolver {

  def transformStackOffset(op: CIL.Op,
                           labelOffsets: Map[String, Int],
                           stackOffsetO: Option[Int]): Either[TranslationError, (Map[String, Int], Option[Int])] = {

    def unstableStackError = Left(InternalError("Unsupported sequence of instructions: stack is unstable"))

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
                                   ctx: MethodTranslationCtx): Either[TranslationError, Map[String, Int]] =
    opcodes
      .foldLeft[Either[TranslationError, (Map[String, Int], Option[Int])]](Right((initLabelOffsets, Some(0)))) {
        case (Right((labelOffsets, stackOffsetO)), op) =>
          for {
            so <- transformStackOffset(op, labelOffsets, stackOffsetO)
            (newLabelOffsets, newStackOffsetO) = so
            deltaOffset <- OpcodeTranslator.deltaOffset(op, ctx)
          } yield (newLabelOffsets, newStackOffsetO.map(_ + deltaOffset))
        case (other, op) => other
      }
      .map(_._1)

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
