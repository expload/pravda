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
import pravda.dotnet.translation.opcode.{CallsTransation, OpcodeTranslator, TypeDetectors}

object Translator {
  private def distinctFunctions(funcs: List[OpcodeTranslator.AdditionalFunction]) = {
    val distinctNames = funcs.map(_.name).distinct
    distinctNames.flatMap(name => funcs.find(_.name == name))
  }

  private def translateMethod(argsCount: Int,
                              localsCount: Int,
                              name: String,
                              cil: List[CIL.Op],
                              signatures: Map[Long, Signatures.Signature],
                              cilData: CilData,
                              local: Boolean,
                              void: Boolean,
                              ctor: Boolean): Either[TranslationError, MethodTranslation] = {

    val ctx = MethodTranslationCtx(argsCount, localsCount, name, signatures, cilData, local, void)

    val opTranslationsE = {
      def doTranslation(cil: List[CIL.Op],
                        offsets: Map[String, Int],
                        stackOffsetO: Option[Int]): Either[TranslationError, List[OpCodeTranslation]] = {
        cil match {
          case op :: _ =>
            for {
              so <- StackOffsetResolver.transformStackOffset(op, offsets, stackOffsetO)
              (_, newStackOffsetO) = so
              asmRes <- OpcodeTranslator.asmOps(cil, newStackOffsetO, ctx)
              (taken, opcodes) = asmRes
              deltaOffset <- OpcodeTranslator.deltaOffset(cil, ctx).map(_._2)
              restTranslations <- doTranslation(cil.drop(taken), offsets, newStackOffsetO.map(_ + deltaOffset))
            } yield OpCodeTranslation(Right(cil.take(taken)), stackOffsetO, opcodes) :: restTranslations

          case _ => Right(List.empty)
        }
      }

      (for {
        convergedOffsets <- StackOffsetResolver.convergeLabelOffsets(cil, ctx)
      } yield doTranslation(cil, convergedOffsets, Some(0))).joinRight
    }

    val clear =
      if (void) {
        List.fill(localsCount + argsCount + (if (ctor) 0 else 1))(asm.Operation(Opcodes.POP))
      } else {
        List.fill(localsCount + argsCount + 1)(List(asm.Operation(Opcodes.SWAP), asm.Operation(Opcodes.POP))).flatten
      }

    val functions = {
      def searchFunctions(ops: List[CIL.Op]): List[OpcodeTranslator.AdditionalFunction] = {
        if (ops.nonEmpty) {
          val (taken, funcs) = OpcodeTranslator.additionalFunctions(ops, ctx)
          funcs ++ searchFunctions(ops.drop(if (taken > 0) taken else 1))
        } else {
          List.empty
        }
      }

      distinctFunctions(searchFunctions(cil))
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
                              if (ctor || local) List(asm.Operation(Opcodes.RET))
                              else List(asm.Operation.Jump(Some("stop"))))
          ),
        functions
      )
  }

  def translateVerbose(rawMethods: List[Method],
                       cilData: CilData,
                       signatures: Map[Long, Signatures.Signature]): Either[TranslationError, Translation] = {

//    val methodsToTypes: Map[Int, TypeDefData] = cilData.tables.methodDefTable.zipWithIndex.flatMap {
//      case (m, i) => cilData.tables.typeDefTable.find(_.methods.exists(_ eq m)).map(i -> _)
//    }.toMap

    val rawMethodNames = rawMethods.indices.map(cilData.tables.methodDefTable(_).name)

    def isLocal(methodIdx: Int): Boolean = false

    val methods = rawMethods.zipWithIndex.filterNot {
      case (_, i) =>
        val name = rawMethodNames(i)
        name == ".ctor" || name == ".cctor" || name == "Main"
    }

    def dotnetToVmTpe(sigType: SigType): Meta.TypeSignature = sigType match {
      case SigType.Void          => Meta.TypeSignature.Null
      case SigType.Boolean       => Meta.TypeSignature.Boolean
      case SigType.I1            => Meta.TypeSignature.Int8
      case SigType.I2            => Meta.TypeSignature.Int16
      case SigType.I4            => Meta.TypeSignature.Int32
      case SigType.U1            => Meta.TypeSignature.Uint8
      case SigType.U2            => Meta.TypeSignature.Uint16
      case SigType.U4            => Meta.TypeSignature.Uint32
      case TypeDetectors.Bytes() => Meta.TypeSignature.Bytes
      // TODO add more types
    }

    lazy val metaMethods = methods.flatMap {
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

    lazy val jumpToMethods = methods.flatMap {
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

    lazy val jumpToConstructor = List(
      asm.Operation.Push(Data.Primitive.Null),
      asm.Operation.Orphan(Opcodes.SEXIST),
      asm.Operation.JumpI(Some("methods")),
      asm.Operation.Call(Some("ctor")),
      asm.Operation.Label("methods")
    )

    lazy val constructorPrefix = List(
      asm.Operation.Label("ctor"),
      asm.Operation.Push(Data.Primitive.Null),
      asm.Operation.Orphan(Opcodes.DUP),
      asm.Operation.Orphan(Opcodes.SPUT)
    )

    lazy val constructorE: Either[TranslationError, Option[ConstructorTranslation]] = {
      val ctorMethod = rawMethods.zipWithIndex.find { case (m, i) => rawMethodNames(i) == ".ctor" }
      ctorMethod match {
        case Some((m, i)) =>
          val localVarSig = m.localVarSigIdx.flatMap(signatures.get)
          val ops = cilData.tables.methodDefTable(i) match {
            case MethodDefData(_, _, name, sigIdx, params) =>
              val isVoid = signatures.get(sigIdx) match {
                case Some(MethodRefDefSig(_, _, _, _, 0, Tpe(tpe, _), params)) =>
                  tpe match {
                    case SigType.Void => true
                    case _            => false
                  }
                case _ => false
              }

              if (!isVoid) {
                Left(InternalError("Constructor can't return value."))
              } else if (params.nonEmpty) {
                Left(InternalError("Constructor shouldn't take arguments."))
              } else {
                translateMethod(
                  0,
                  localVarSig
                    .map {
                      case LocalVarSig(types) => types.length
                      case _                  => 0
                    }
                    .getOrElse(0),
                  name,
                  BranchTransformer.transformBranches(m.opcodes),
                  signatures,
                  cilData,
                  false,
                  true,
                  true
                )
              }
          }
          ops.map(o => Some(ConstructorTranslation(jumpToConstructor, constructorPrefix, o)))
        case None => Right(None)
      }
    }

    lazy val methodsOpsE: Either[TranslationError, List[MethodTranslation]] = methods.map {
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
              cilData,
              isLocal(i),
              isVoid,
              false
            )
        }
    }.sequence

    for {
      _ <- if (rawMethodNames.contains(".cctor")) Left(InternalError("Static constructor isn't allowed."))
      else Right(())
      methodsOps <- methodsOpsE
      constructor <- constructorE
    } yield
      Translation(
        metaMethods ++ jumpToMethods :+ asm.Operation.Jump(Some("stop")),
        methodsOps,
        constructor,
        distinctFunctions((constructor.map(_.ctor).toList ++ methodsOps).flatMap(_.additionalFunctions)),
        List(asm.Operation.Label("stop"))
      )
  }

  def translationToAsm(t: Translation): List[asm.Operation] =
    t.constructor.map(_.jumpToConstructor).getOrElse(List.empty) ++
      t.jumpToMethods ++
      t.methods.flatMap(_.opcodes.flatMap(_.asmOps)) ++
      t.constructor.map(_.ctorPrefix).getOrElse(List.empty) ++
      t.constructor.map(_.ctor.opcodes.flatMap(_.asmOps).drop(1)).getOrElse(List.empty) ++
      t.functions.flatMap { case OpcodeTranslator.AdditionalFunction(name, ops) => asm.Operation.Label(name) :: ops } ++
      t.finishOps

  def translateAsm(rawMethods: List[Method],
                   cilData: CilData,
                   signatures: Map[Long, Signatures.Signature]): Either[TranslationError, List[asm.Operation]] =
    translateVerbose(rawMethods, cilData, signatures).map(translationToAsm)
}
