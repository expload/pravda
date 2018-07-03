package pravda.dotnet.translation.opcode

import pravda.dotnet.parsers.CIL
import pravda.vm.asm
import pravda.dotnet.translation.data._

import scala.annotation.tailrec

// We can't compute delta offset from generated asm code,
// because in general asm generation requires stack offset,
// which itself requires delta offsets
trait OpcodeTranslator {
  def deltaOffset(op: CIL.Op, ctx: MethodTranslationCtx): Either[TranslationError, Int]

  def translate(op: CIL.Op,
                stackOffsetO: Option[Int],
                ctx: MethodTranslationCtx): Either[TranslationError, List[asm.Operation]]
}

object OpcodeTranslator {

  val translators: List[OpcodeTranslator] =
    List(SimpleTranslations, ArgsLocalsTranslations, FieldsTranslation, JumpsTranslation, CallsTransation)

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

  def deltaOffset(op: CIL.Op, ctx: MethodTranslationCtx): Either[TranslationError, Int] =
    findAndReturn(translators, (t: OpcodeTranslator) => t.deltaOffset(op, ctx), notUnknownOpcode)
      .getOrElse(Left(UnknownOpcode))

  def translate(op: CIL.Op,
                stackOffsetO: Option[Int],
                ctx: MethodTranslationCtx): Either[TranslationError, List[asm.Operation]] =
    findAndReturn(translators, (t: OpcodeTranslator) => t.translate(op, stackOffsetO, ctx), notUnknownOpcode)
      .getOrElse(Left(UnknownOpcode))

}
