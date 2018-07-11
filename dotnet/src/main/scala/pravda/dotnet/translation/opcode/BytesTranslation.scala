package pravda.dotnet.translation.opcode
import com.google.protobuf.ByteString
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.translation.data.{MethodTranslationCtx, TranslationError, UnknownOpcode}
import pravda.vm.asm.Operation
import pravda.vm.{Data, Opcodes, asm}

object BytesTranslation extends OneToManyTranslatorOnlyAsm {

  private var loopLabelId: Int = 0

  def arrayToBytesConversion(loopLabel: String): List[asm.Operation] = List(
    Operation.Orphan(Opcodes.DUP),
    Operation.Orphan(Opcodes.LENGTH),
    Operation.Push(Data.Primitive.Bytes(ByteString.EMPTY)),
    pushInt(0),
    Operation.Label(loopLabel),
    pushInt(4),
    Operation.Orphan(Opcodes.DUPN),
    pushInt(2),
    Operation.Orphan(Opcodes.DUPN),
    Operation.Orphan(Opcodes.ARRAY_GET),
    pushType(Data.Type.Bytes),
    Operation.Orphan(Opcodes.CAST),
    pushInt(3),
    Operation.Orphan(Opcodes.DUPN),
    Operation.Orphan(Opcodes.SWAP),
    Operation.Orphan(Opcodes.CONCAT),
    pushInt(3),
    Operation.Orphan(Opcodes.SWAPN),
    Operation.Orphan(Opcodes.POP),
    pushInt(1),
    Operation.Orphan(Opcodes.ADD),
    Operation.Orphan(Opcodes.DUP),
    pushInt(4),
    Operation.Orphan(Opcodes.DUPN),
    Operation.Orphan(Opcodes.GT),
    Operation.JumpI(Some(loopLabel)),
    Operation.Orphan(Opcodes.POP),
    Operation.Orphan(Opcodes.SWAP),
    Operation.Orphan(Opcodes.POP),
    Operation.Orphan(Opcodes.SWAP),
    Operation.Orphan(Opcodes.POP)
  )

  override def asmOpsOne(op: Op,
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[TranslationError, List[asm.Operation]] = op match {
    case NewObj(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), ".ctor", signatureIdx)) =>
      loopLabelId += 1
      Right(arrayToBytesConversion(s"a2b_loop_$loopLabelId")) // FIXME nasty hack
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "get_Item", _)) =>
      Right(List(Operation.Orphan(Opcodes.ARRAY_GET)))
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "Slice", _)) =>
      Right(
        List(pushInt(2),
             Operation(Opcodes.DUPN),
             Operation(Opcodes.ADD),
             Operation(Opcodes.SWAP),
             Operation(Opcodes.SLICE))
      )
    case _ => Left(UnknownOpcode)
  }
}
