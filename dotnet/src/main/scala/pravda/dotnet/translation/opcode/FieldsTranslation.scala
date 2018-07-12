package pravda.dotnet.translation.opcode

import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures._
import pravda.dotnet.translation.data._
import pravda.vm.{Opcodes, asm}

case object FieldsTranslation extends OneToManyTranslatorOnlyAsm {

  override def asmOpsOne(op: CIL.Op,
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[TranslationError, List[asm.Operation]] = {

    def loadField(name: String, sigIdx: Long): List[asm.Operation] = { // FIXME should process static fields too
      lazy val defaultLoad = List(
        pushString(name),
        asm.Operation(Opcodes.SGET)
      )

      ctx.signatures.get(sigIdx) match {
        case Some(FieldSig(tpe)) =>
          tpe match {
            case SigType.Generic(TypeDetectors.Mapping(), _) =>
              List(pushString(name))
            case TypeDetectors.Mapping() =>
              List(pushString(name))
            case TypeDetectors.Address() if name == "sender" =>
              List(asm.Operation(Opcodes.FROM))
            case _ => defaultLoad
          }
        case _ => defaultLoad
      }
    }

    def storeField(name: String, sigIdx: Long): List[asm.Operation] = { // FIXME should process static fields too
      lazy val defaultStore = List(
        pushString(name),
        asm.Operation(Opcodes.SPUT)
      )

      ctx.signatures.get(sigIdx) match {
        case Some(FieldSig(tpe)) =>
          tpe match {
            case TypeDetectors.Mapping() =>
              List(asm.Operation(Opcodes.STOP)) // error user shouldn't modify mappings
            case TypeDetectors.Mapping() if name == "sender" =>
              List(asm.Operation(Opcodes.STOP)) // error user shouldn't modify sender address
            case _ => defaultStore
          }
        case _ => defaultStore
      }
    }

    op match {
      case LdSFld(FieldData(_, name, sig)) => Right(loadField(name, sig))
      case LdFld(FieldData(_, name, sig))  => Right(loadField(name, sig))
      case StSFld(FieldData(_, name, sig)) => Right(storeField(name, sig))
      case StFld(FieldData(_, name, sig))  => Right(storeField(name, sig))
      case _                               => Left(UnknownOpcode)
    }
  }
}
