package pravda.dotnet.translation.opcode
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.translation.data.{InternalError, MethodTranslationCtx, TranslationError, UnknownOpcode}
import pravda.dotnet.translation.opcode.OpcodeTranslator.Taken
import pravda.vm.asm.Operation

object MappingInitializationTranslation extends OpcodeTranslatorOnlyAsm {
  override def asmOps(ops: List[CIL.Op],
                      stackOffsetO: Option[Int],
                      ctx: MethodTranslationCtx): Either[TranslationError, (Taken, List[Operation])] =
    ops.take(2) match {
      case List(NewObj(MemberRefData(TypeSpecData(signIdx), ".ctor", _)), StFld(FieldData(_, name, _))) =>
        val res = for {
          parentSig <- ctx.signatures.get(signIdx)
        } yield {
          if (CallsTransation.detectMapping(parentSig)) {
            Right(
              (2,
               List.empty)
            )
          } else {
            Left(UnknownOpcode)
          }
        }

        res.getOrElse(Left(InternalError("Invalid signatures")))
      case _ => Left(UnknownOpcode)
    }
}
