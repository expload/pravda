package pravda.dotnet.translation

import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.{CIL, Signatures}
import pravda.dotnet.parsers.Signatures._
import pravda.dotnet.translation.data._
import pravda.dotnet.translation.jumps.{BranchTransformer, StackOffsetResolver}
import pravda.vm.{Data, Meta, Opcodes, asm}
import pravda.dotnet.translation.opcode.{CallsTransation, OpcodeTranslator, TypeUtils}

object Translator {
  private def translateMethod(argsCount: Int,
                              localsCount: Int,
                              name: String,
                              cil: List[CIL.Op],
                              signatures: Map[Long, Signatures.Signature],
                              local: Boolean,
                              void: Boolean): Either[TranslationError, MethodTranslation] = {

    val ctx = MethodTranslationCtx(argsCount, localsCount, name, signatures, local, void)

    val opTranslationsE = {
      val res = for {
        convergedOffsets <- StackOffsetResolver.convergeLabelOffsets(cil, ctx)
      } yield
        cil
          .foldLeft[Either[TranslationError, (List[OpCodeTranslation], Option[Int])]](Right((List.empty, Some(0)))) {
            case (Right((acc, stackOffsetO)), op) =>
              for {
                so <- StackOffsetResolver.transformStackOffset(op, convergedOffsets, stackOffsetO)
                (_, newStackOffsetO) = so
                opcodes <- OpcodeTranslator.translate(op, newStackOffsetO, ctx)
                deltaOffset <- OpcodeTranslator.deltaOffset(op, ctx)
              } yield (OpCodeTranslation(Right(op), stackOffsetO, opcodes) :: acc, newStackOffsetO.map(_ + deltaOffset))
            case (other, op) => other
          }
          .map(_._1.reverse)

      res.joinRight
    }

    val clear =
      if (void) {
        List.fill(localsCount + argsCount + 1)(asm.Operation(Opcodes.POP))
      } else {
        List.fill(localsCount + argsCount + 1)(List(asm.Operation(Opcodes.SWAP), asm.Operation(Opcodes.POP))).flatten
      }

    for {
      opTranslations <- opTranslationsE
    } yield
      MethodTranslation(
        name,
        argsCount,
        localsCount,
        local,
        void,
        List(
          OpCodeTranslation(Left("method name"), None, List(asm.Operation.Label("method_" + name))),
          OpCodeTranslation(Left("local vars"), None, List.fill(localsCount)(opcode.pushInt(0))) // FIXME Should be replaced by proper value for local var type
        ) ++ opTranslations ++
          List(
            OpCodeTranslation(Left("local vars clearing"), None, clear),
            OpCodeTranslation(Left("end of a method"),
                              None,
                              if (local) List(asm.Operation(Opcodes.RET)) else List(asm.Operation.Jump(Some("stop"))))
          )
      )
  }

  def translateVerbose(rawMethods: List[Method],
                       cilData: CilData,
                       signatures: Map[Long, Signatures.Signature]): Either[TranslationError, Translation] = {
    val methodsToTypes: Map[Int, TypeDefData] = cilData.tables.methodDefTable.zipWithIndex.flatMap {
      case (m, i) => cilData.tables.typeDefTable.find(_.methods.exists(_ eq m)).map(i -> _)
    }.toMap

    def isLocal(methodIdx: Int): Boolean = {
      methodsToTypes.get(methodIdx) match {
        case Some(TypeDefData(_, _, "io.mytc.pravda", _, _, _)) => true
        case _                                                  => false
      }
    }

    val methods = rawMethods.zipWithIndex.filterNot {
      case (m, i) =>
        val name = cilData.tables.methodDefTable(i).name
        name == ".ctor" || name == ".cctor"
    }

    def dotnetToVmTpe(sigType: SigType): Meta.TypeSignature = sigType match {
      case SigType.Void        => Meta.TypeSignature.Null
      case SigType.Boolean     => Meta.TypeSignature.Boolean
      case SigType.I1          => Meta.TypeSignature.Int8
      case SigType.I2          => Meta.TypeSignature.Int16
      case SigType.I4          => Meta.TypeSignature.Int32
      case SigType.U1          => Meta.TypeSignature.Uint8
      case SigType.U2          => Meta.TypeSignature.Uint16
      case SigType.U4          => Meta.TypeSignature.Uint32
      case TypeUtils.Address() => Meta.TypeSignature.BigInt
      // TODO add more types
    }

    val metaMethods = methods.flatMap {
      case (m, i) =>
        if (!isLocal(i)) {
          val name = cilData.tables.methodDefTable(i).name
          for {
            methodSign <- signatures.get(cilData.tables.methodDefTable(i).signatureIdx)
            methodTpe <- CallsTransation.methodType(methodSign)
            argTpes <- CallsTransation.methodParams(methodSign)
          } yield
            asm.Operation.Meta(
              Meta.MethodSignature(
                name,
                dotnetToVmTpe(methodTpe),
                argTpes.map(tpe => (None, dotnetToVmTpe(tpe.tpe)))
              )
            )
        } else {
          None
        }
    }

    val jumpToMethods = methods.flatMap {
      case (m, i) =>
        if (!isLocal(i)) {
          val name = cilData.tables.methodDefTable(i).name
          List(
            asm.Operation(Opcodes.DUP),
            asm.Operation.Push(Data.Primitive.Utf8(name)),
            asm.Operation(Opcodes.EQ),
            asm.Operation.JumpI(Some("method_" + name))
          )
        } else {
          List.empty
        }
    }

    val methodsOpsE: Either[TranslationError, List[MethodTranslation]] = methods.map {
      case (m, i) =>
        val localVarSig = m.localVarSigIdx.flatMap(signatures.get)
        cilData.tables.methodDefTable(i) match {
          case MethodDefData(_, _, name, sigIdx, params) =>
            val isVoid = signatures.get(sigIdx) match {
              case Some(MethodRefDefSig(_, _, _, _, 0, Tpe(tpe, _), params)) =>
                tpe match {
                  case SigType.Void => true
                  case _            => false
                }
              case _ => false
            }

            translateMethod(
              params.length, // FIXME should properly handle static methods
              localVarSig
                .map {
                  case LocalVarSig(types) => types.length
                  case _                  => 0
                }
                .getOrElse(0),
              name,
              BranchTransformer.transformBranches(m.opcodes),
              signatures,
              isLocal(i),
              isVoid
            )
        }
    }.sequence

    for {
      methodsOps <- methodsOpsE
    } yield
      Translation(metaMethods ++ jumpToMethods :+ asm.Operation.Jump(Some("stop")),
                  methodsOps,
                  List(asm.Operation.Label("stop")))
  }

  def translateAsm(rawMethods: List[Method],
                   cilData: CilData,
                   signatures: Map[Long, Signatures.Signature]): Either[TranslationError, List[asm.Operation]] =
    for {
      t <- translateVerbose(rawMethods, cilData, signatures)
    } yield t.jumpToMethods ++ t.methods.flatMap(_.opcodes.flatMap(_.asmOps)) ++ t.finishOps
}
