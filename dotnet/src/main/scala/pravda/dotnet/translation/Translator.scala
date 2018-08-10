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
import pravda.dotnet.data.{Method, TablesData}
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.{CIL, Signatures}
import pravda.dotnet.parsers.Signatures._
import pravda.dotnet.translation.data._
import pravda.dotnet.translation.jumps.{BranchTransformer, StackOffsetResolver}
import pravda.vm.{Data, Meta, Opcodes, asm}
import pravda.dotnet.translation.opcode.{CallsTransation, OpcodeTranslator}
import pravda.vm.Meta.TranslatorMark

import scala.collection.mutable.ListBuffer

object Translator {

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
    case SigType.String        => Meta.TypeSignature.Utf8
    // TODO add more types
  }

  private def distinctFunctions(funcs: List[OpcodeTranslator.HelperFunction]) = {
    val distinctNames = funcs.map(_.name).distinct
    distinctNames.flatMap(name => funcs.find(_.name == name))
  }

  private def translateMethod(
      argsCount: Int,
      localsCount: Int,
      name: String,
      cil: List[CIL.Op],
      signatures: Map[Long, Signatures.Signature],
      cilData: CilData,
      void: Boolean,
      func: Boolean,
      kind: String,
      debugInfo: Option[MethodDebugInformationData]): Either[TranslationError, MethodTranslation] = {

    val ctx = MethodTranslationCtx(argsCount, localsCount, name, signatures, cilData, void, func, debugInfo)

    val opTranslationsE = {
      def doTranslation(cil: List[CIL.Op],
                        offsets: Map[String, Int]): Either[TranslationError, List[OpCodeTranslation]] = {

        var curCil = cil
        var stackOffsetO: Option[Int] = Some(0)
        var cilOffset = 0
        val res = ListBuffer[OpCodeTranslation]()
        var errorE: Either[InnerTranslationError, Unit] = Right(())

        while (curCil.nonEmpty && errorE.isRight) {

          errorE = for {
            so <- StackOffsetResolver.transformStackOffset(curCil.head, offsets, stackOffsetO)
            (_, newStackOffsetO) = so
            asmRes <- OpcodeTranslator.asmOps(curCil, newStackOffsetO, ctx)
            (taken, opcodes) = asmRes
            (takenCil, restCil) = curCil.splitAt(taken)
            takenOffset = takenCil.map(_.size).sum
            deltaOffset <- OpcodeTranslator.deltaOffset(curCil, ctx).map(_._2)
          } yield {
            res += OpCodeTranslation(
              Right(restCil),
              debugInfo
                .map(DebugInfo.searchForSourceMarks(_, cilOffset, cilOffset + takenOffset))
                .getOrElse(List.empty),
              Some(cilOffset),
              stackOffsetO,
              opcodes
            )

            curCil = restCil
            stackOffsetO = newStackOffsetO.map(_ + deltaOffset)
            cilOffset += takenOffset
          }
        }

        errorE match {
          case Left(err) => Left(TranslationError(err, debugInfo.flatMap(DebugInfo.firstSourceMark(_, cilOffset))))
          case Right(()) => Right(res.toList)
        }
      }

      (for {
        convergedOffsets <- StackOffsetResolver.convergeLabelOffsets(cil, ctx)
      } yield doTranslation(cil, convergedOffsets)).joinRight
    }

    val clear =
      if (void) {
        List.fill(localsCount + argsCount + (if (func) 0 else 1))(asm.Operation(Opcodes.POP))
      } else {
        List
          .fill(localsCount + argsCount + (if (func) 0 else 1))(
            List(asm.Operation(Opcodes.SWAP), asm.Operation(Opcodes.POP)))
          .flatten
      }

    val functions = {
      def searchFunctions(ops: List[CIL.Op]): List[OpcodeTranslator.HelperFunction] = {
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
        void,
        func,
        List(
          OpCodeTranslation(Left(s"$name $kind"), List.empty, None, None, List(asm.Operation.Label(s"${kind}_$name"))),
          OpCodeTranslation(Left(s"$name local vars definition"),
                            List.empty,
                            None,
                            None,
                            List.fill(localsCount)(asm.Operation.Push(Data.Primitive.Null))),
          OpCodeTranslation(Left(s"$name $kind body"), List.empty, None, None, List.empty)
        )
          ++ opTranslations ++
          List(
            OpCodeTranslation(Left(s"$name local vars clearing"), List.empty, None, None, clear),
            OpCodeTranslation(Left(s"end of $name $kind"),
                              List.empty,
                              None,
                              None,
                              if (func) List(asm.Operation(Opcodes.RET))
                              else List(asm.Operation.Jump(Some("stop"))))
          ),
        functions
      )
  }

  def translateVerbose(methods: List[Method],
                       cilData: CilData,
                       signatures: Map[Long, Signatures.Signature],
                       pdbTables: Option[TablesData] = None): Either[TranslationError, Translation] = {

    val programClassE = {
      val classesWithProgramAttribute = cilData.tables.customAttributeTable.collect {
        case CustomAttributeData(cls, MemberRefData(TypeRefData(_, "Program", "Com.Expload"), ".ctor", _)) => cls
      }

      if (classesWithProgramAttribute.length != 1) {
        Left(TranslationError(InternalError("You must define exactly one class with [Program] attribute"), None))
      } else {
        Right(classesWithProgramAttribute(0))
      }
    }

    val methodsToTypes: Map[Int, TypeDefData] = cilData.tables.methodDefTable.zipWithIndex.flatMap {
      case (m, i) => cilData.tables.typeDefTable.find(_.methods.exists(_ eq m)).map(i -> _)
    }.toMap

    def isCtor(methodIdx: Int): Boolean = {
      val methodRow = cilData.tables.methodDefTable(methodIdx)
      methodRow.name == ".ctor" && (methodRow.flags & 0x1800) != 0 // maybe the mask should be different (see 252-nd page in spec)
    }

    def isCctor(methodIdx: Int): Boolean = {
      val methodRow = cilData.tables.methodDefTable(methodIdx)
      methodRow.name == ".cctor" && (methodRow.flags & 0x1810) != 0 // maybe the mask should be different (see 252-nd page in spec)
    }

    def isMain(methodIdx: Int): Boolean = {
      val methodRow = cilData.tables.methodDefTable(methodIdx)
      methodRow.name == "Main" && (methodRow.flags & 0x10) != 0
    }

    def isPrivate(methodIdx: Int): Boolean =
      (cilData.tables.methodDefTable(methodIdx).flags & 0x7) == 0x1

    def isStatic(methodIdx: Int): Boolean =
      (cilData.tables.methodDefTable(methodIdx).flags & 0x10) != 0

    val programMethodsE = (for {
      programClass <- programClassE
    } yield {
      methods.zipWithIndex.filter {
        case (_, i) =>
          (methodsToTypes(i) eq programClass) && !isPrivate(i) && !isCtor(i) && !isCctor(i) && !isMain(i)
      }
    }).filterOrElse(_.forall { case (m, i) => !isStatic(i) }, Left(TranslationError(InternalError("Static program methods are forbidden"), None)))

    val programFuncsE = for {
      programClass <- programClassE
    } yield {
      methods.zipWithIndex.filter {
        case (_, i) =>
          (methodsToTypes(i) eq programClass) && isPrivate(i) && !isCtor(i) && !isCctor(i) && !isMain(i)
      }
    }

    val programConstructorE = for {
      programClass <- programClassE
      ctor <- {
        val ctors = methods.zipWithIndex.filter {
          case (_, i) =>
            (methodsToTypes(i) eq programClass) && isCtor(i)
        }

        if (ctors.length > 1) {
          Left(TranslationError(InternalError("You must define exactly one constructor for the program"), None))
        } else if (ctors.length == 1) {
          val ctor = ctors.head
          val ctorRow = cilData.tables.methodDefTable(ctor._2)
          if (ctorRow.params.nonEmpty) {
            Left(TranslationError(InternalError("Program constructor must not take arguments"), None))
          } else {
            Right(Some(ctor))
          }
        } else {
          Right(None)
        }
      }
    } yield ctor

    val structFuncsE = for {
      programClass <- programClassE
    } yield {
      methods.zipWithIndex.filter {
        case (_, i) =>
          !(methodsToTypes(i) eq programClass) && !isCtor(i) && !isCctor(i) && !isMain(i)
      }
    }


//    lazy val metaMethods = methods.flatMap {
//      case (m, i) =>
//        val name = cilData.tables.methodDefTable(i).name
//        for {
//          methodSign <- signatures.get(cilData.tables.methodDefTable(i).signatureIdx)
//          methodTpe <- CallsTransation.methodType(methodSign)
//          argTpes <- CallsTransation.methodParams(methodSign)
//        } yield
//          asm.Operation.Meta(
//            Meta.MethodSignature(
//              name,
//              dotnetToVmTpe(methodTpe),
//              argTpes.map(tpe => dotnetToVmTpe(tpe.tpe))
//            )
//          )
//    }

    lazy val jumpToMethods = for {
      programMethods <- programMethodsE
    } yield {
      programMethods.flatMap {
        case (m, i) =>
          val name = cilData.tables.methodDefTable(i).name
          asm(
            s"""
              |dup
              |push "$name"
              |eq
              |jumpi "method_$name"
            """.stripMargin)
      }
    }

    lazy val constructorE: Either[TranslationError, Option[MethodTranslation]] = for {
      ctor <- programConstructorE
    } yield {
      ctor match {
        case Some((m, i)) =>
          val ctorRow = cilData.tables.methodDefTable(i)
          Some(translateMethod(
            0,
            MethodExtractors.localVariables(m, signatures).fold(0)(_.length),
            "ctor",
            BranchTransformer.transformBranches(m.opcodes, "ctor"),
            signatures,
            cilData,
            void = true,
            func = true,
            "method",
            pdbTables.map(_.methodDebugInformationTable(i))
          )
          )
        case None => Right(None)
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
              BranchTransformer.transformBranches(m.opcodes, name),
              signatures,
              cilData,
              isVoid,
              false,
              "method",
              pdbTables.map(_.methodDebugInformationTable(i))
            )
        }
    }.sequence

    lazy val funcOpsE: Either[TranslationError, List[MethodTranslation]] = funcs.map {
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
              params.length,
              localVarSig
                .map {
                  case LocalVarSig(types) => types.length
                  case _                  => 0
                }
                .getOrElse(0),
              name,
              BranchTransformer.transformBranches(m.opcodes, name),
              signatures,
              cilData,
              isVoid,
              true,
              "func",
              pdbTables.map(_.methodDebugInformationTable(i))
            )
        }
    }.sequence

    for {
      _ <- if (false /*rawMethodNames.contains(".cctor")*/ ) {
        Left(TranslationError(InternalError("Static constructor isn't allowed."), None))
      } else {
        Right(())
      }
      methodsOps <- methodsOpsE
      constructor <- constructorE
      funcOps <- funcOpsE
    } yield
      Translation(
        metaMethods ++ jumpToMethods ++ List(
          asm.Operation.Push(Data.Primitive.Utf8("Wrong method name")),
          asm.Operation(Opcodes.THROW)
        ),
        methodsOps ++ constructor.toList,
        funcOps,
        distinctFunctions((funcOps ++ methodsOps).flatMap(_.additionalFunctions)),
        List(asm.Operation.Label("stop"))
      )
  }

  def translationToAsm(t: Translation): List[asm.Operation] = {
    def translatorMark(s: String): List[asm.Operation] =
      List(asm.Operation.Meta(TranslatorMark(s)))

    def opcodeToAsm(opCodeTranslation: OpCodeTranslation): List[asm.Operation] = {
      val translatorMarkL =
        opCodeTranslation.source.swap.map(s => translatorMark(s)).getOrElse(List.empty)
      val sourceMarks = opCodeTranslation.sourceMarks.map(asm.Operation.Meta(_))

      translatorMarkL ++ sourceMarks ++ opCodeTranslation.asmOps
    }

    translatorMark("jump to methods") ++
      t.jumpToMethods ++
      t.methods.flatMap(_.opcodes.flatMap(opcodeToAsm)) ++
      t.funcs.flatMap(_.opcodes.flatMap(opcodeToAsm)) ++
      translatorMark("helper functions") ++
      t.helperFunctions.flatMap { case OpcodeTranslator.HelperFunction(name, ops) => asm.Operation.Label(name) :: ops } ++
      t.finishOps
  }

  def translateAsm(rawMethods: List[Method],
                   cilData: CilData,
                   signatures: Map[Long, Signatures.Signature],
                   pdbTables: Option[TablesData] = None): Either[TranslationError, List[asm.Operation]] =
    translateVerbose(rawMethods, cilData, signatures, pdbTables).map(translationToAsm)
}
