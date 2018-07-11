package pravda.dotnet.translation.opcode
import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.translation.data._
import pravda.vm.{Opcodes, asm}

case object JumpsTranslation extends OneToManyTranslatorOnlyAsm {

  val labelStackOffset = 0
  val jumpStackOffset = 0
  val jumpIStackOffset = -1

  override def asmOpsOne(op: CIL.Op,
                         stackOffset: Option[Int],
                         ctx: MethodTranslationCtx): Either[TranslationError, List[asm.Operation]] =
    op match {
      case Jump(label) => Right(List(asm.Operation.Jump(Some(label))))
      case JumpI(label) =>
        Right(List(pushInt(1), asm.Operation(Opcodes.EQ), asm.Operation.JumpI(Some(label))))
      case Label(label) => Right(List(asm.Operation.Label(label)))
      case _            => Left(UnknownOpcode)
    }
}
