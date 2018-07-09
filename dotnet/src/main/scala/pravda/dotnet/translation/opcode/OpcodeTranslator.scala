package pravda.dotnet.translation.opcode

import pravda.dotnet.parsers.CIL
import pravda.vm.asm
import pravda.dotnet.translation.data._

import scala.annotation.tailrec

// We can't compute delta offset from generated asm code,
// because in general asm generation requires stack offset,
// which itself requires delta offsets
trait OpcodeTranslator {
  def translate(ops: List[CIL.Op],
                stackOffsetO: Option[Int],
                ctx: MethodTranslationCtx): Either[TranslationError, OpcodeTranslator.TranslationResult]
}

object OneToManyTranslator {
  final case class TranslationResult(asmOps: List[asm.Operation], deltaOffset: Int)
}

trait OneToManyTranslator extends OpcodeTranslator {

  def translateOne(op: CIL.Op,
                   stackOffsetO: Option[Int],
                   ctx: MethodTranslationCtx): Either[TranslationError, OneToManyTranslator.TranslationResult]

  def translate(ops: List[CIL.Op],
                stackOffsetO: Option[Int],
                ctx: MethodTranslationCtx): Either[TranslationError, OpcodeTranslator.TranslationResult] =
    ops match {
      case head :: tail =>
        translateOne(head, stackOffsetO, ctx).map(res =>
          OpcodeTranslator.TranslationResult(tail, res.asmOps, res.deltaOffset))
      case _ => Right(OpcodeTranslator.TranslationResult(Nil, Nil, 0))
    }
}

trait OneToManySeparateTranslator extends OneToManyTranslator {
  def deltaOffset(op: CIL.Op, ctx: MethodTranslationCtx): Either[TranslationError, Int]

  def asmOps(op: CIL.Op,
                stackOffsetO: Option[Int],
                ctx: MethodTranslationCtx): Either[TranslationError, List[asm.Operation]]

  def translateOne(op: CIL.Op,
                   stackOffsetO: Option[Int],
                   ctx: MethodTranslationCtx): Either[TranslationError, OneToManyTranslator.TranslationResult] =
    for {
      ops <- asmOps(op, stackOffsetO, ctx)
      offset <- deltaOffset(op, ctx)
    } yield OneToManyTranslator.TranslationResult(ops, offset)
}

object OpcodeTranslator {

  final case class TranslationResult(restOps: List[CIL.Op], asmOps: List[asm.Operation], deltaOffset: Int)

  val translators: List[OpcodeTranslator] =
    List(SimpleTranslations,
         ArgsLocalsTranslations,
         FieldsTranslation,
         JumpsTranslation,
         StringTranslation,
         CallsTransation)

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

  def translate(ops: List[CIL.Op],
                stackOffsetO: Option[Int],
                ctx: MethodTranslationCtx): Either[TranslationError, TranslationResult] =
    findAndReturn(translators, (t: OpcodeTranslator) => t.translate(ops, stackOffsetO, ctx), notUnknownOpcode)
      .getOrElse(Left(UnknownOpcode))

}
