package pravda.dotnet.translation.opcode
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._
import pravda.dotnet.translation.data._
import pravda.vm.{Data, Opcodes, asm}

case object CallsTransation extends OneToManyTranslator {

  private val mappingsMethods = Set("get", "getDefault", "exists", "put")

  private def detectMapping(sig: Signature): Boolean = {
    sig match {
      case TypeSig(Tpe(Generic(TypeDetectors.Mapping(), _), _)) => true
      case _                                                    => false
    }
  }

  def methodType(sig: Signature): Option[SigType] =
    sig match {
      case MethodRefDefSig(_, _, _, _, 0, Tpe(tpe, _), _) => Some(tpe)
      case _                                              => None
    }

  def methodParams(sig: Signature): Option[List[Tpe]] =
    sig match {
      case MethodRefDefSig(_, _, _, _, 0, _, params) => Some(params)
      case _                                         => None
    }

  private def methodParamsCount(sig: Signature): Int =
    methodParams(sig).map(_.length).getOrElse(0)

  def isMethodVoid(sig: Signature): Boolean = methodType(sig) match {
    case Some(SigType.Void) => true
    case _                  => false
  }

  override def deltaOffsetOne(op: CIL.Op, ctx: MethodTranslationCtx): Either[TranslationError, Int] = {
    op match {
      // case Call(MemberRefData(TypeRefData(6, "String", "System"), "Concat", methodSigIdx)) =>

      case CallVirt(MemberRefData(TypeSpecData(parentSigIdx), name, methodSigIdx)) =>
        val res = for {
          parentSig <- ctx.signatures.get(parentSigIdx)
          methodSig <- ctx.signatures.get(methodSigIdx)
        } yield {
          lazy val paramsCnt = methodParamsCount(methodSig)
          lazy val offset = if (isMethodVoid(methodSig)) -paramsCnt - 1 else -paramsCnt - 1 + 1 // FIXME static methods are not supported

          if (detectMapping(parentSig) && mappingsMethods.contains(name)) {
            Right(offset)
          } else {
            Left(UnknownOpcode)
          }
        }

        res.getOrElse(Left(InternalError("Invalid signatures")))

      case _ => Left(UnknownOpcode)
    }
  }

  override def asmOpsOne(op: CIL.Op,
                      stackOffsetO: Option[Int],
                      ctx: MethodTranslationCtx): Either[TranslationError, List[asm.Operation]] = {

    op match {
      case CallVirt(MemberRefData(TypeSpecData(parentSigIdx), name, methodSigIdx)) =>
        val res = for {
          parentSig <- ctx.signatures.get(parentSigIdx)
          methodSig <- ctx.signatures.get(methodSigIdx)
        } yield {
          if (detectMapping(parentSig)) {
            name match {
              case "get" =>
                Right(List(asm.Operation(Opcodes.SWAP), asm.Operation(Opcodes.CONCAT), asm.Operation(Opcodes.SGET)))
              case "getDefault" => Right(List(asm.Operation.Call(Some("method_getDefault"))))
              case "exists" =>
                Right(
                  List(asm.Operation(Opcodes.SWAP), asm.Operation(Opcodes.CONCAT), asm.Operation(Opcodes.SEXIST)) ++ cast(
                    Data.Type.Int8))
              case "put" =>
                Right(
                  List(
                    pushInt(2),
                    asm.Operation(Opcodes.DUPN),
                    pushInt(4),
                    asm.Operation(Opcodes.DUPN),
                    asm.Operation(Opcodes.CONCAT),
                    asm.Operation(Opcodes.SWAP),
                    asm.Operation(Opcodes.SPUT),
                    asm.Operation(Opcodes.POP),
                    asm.Operation(Opcodes.POP)
                  )
                )
              case _ => Left(UnknownOpcode)
            }
          } else {
            Left(UnknownOpcode)
          }
        }

        res.getOrElse(Left(InternalError("Invalid signatures")))

      case _ => Left(UnknownOpcode)
    }
  }
}
