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
import pravda.dotnet.translation.opcode.{CallsTransation, FieldsTranslation, OpcodeTranslator}
import pravda.vm.Meta.TranslatorMark
import pravda.vm.asm.Operation

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

  private def vtableInit(typeDefData: TypeDefData, tctx: TranslationCtx): List[asm.Operation] = {

    def collectMethodNames(typeDefData: TypeDefData, usedMethods: Vector[String]): Vector[(String, String)] = {
      val methodNames = typeDefData.methods.flatMap {
        case MethodDefData(_, _, flags, name, sigIdx, _) =>
          if (CallsTransation.isMethodVirtual(flags)) {
            Some(CallsTransation.fullMethodName(name, tctx.signatures.get(sigIdx)))
          } else {
            None
          }
      }

      val notUsedMethods = methodNames.filter(name => !usedMethods.contains(name))
      val notUsedMethodsWithType = notUsedMethods.map((_, CallsTransation.fullTypeDefName(typeDefData)))

      typeDefData.parent match {
        case typeDef: TypeDefData =>
          notUsedMethodsWithType ++ collectMethodNames(typeDef, usedMethods ++ notUsedMethods)
        case _ => notUsedMethodsWithType
      }
    }

    collectMethodNames(typeDefData, Vector.empty).flatMap {
      case (name, tpe) =>
        List(
          asm.Operation(Opcodes.DUP),
          asm.Operation.PushRef(s"func_$tpe.$name"),
          asm.Operation.StructMut(Some(Data.Primitive.Utf8(name)))
        )
    }.toList
  }

  private def translateMethod(cil: List[CIL.Op],
                              name: String,
                              kind: String,
                              id: Int,
                              argsCount: Int,
                              localsCount: Int,
                              void: Boolean,
                              func: Boolean,
                              static: Boolean,
                              struct: Option[String],
                              debugInfo: Option[MethodDebugInformationData],
                              tctx: TranslationCtx): Either[TranslationError, MethodTranslation] = {

    val ctx =
      MethodTranslationCtx(tctx, argsCount, localsCount, name, kind, void, func, static, struct, debugInfo)

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

    val clear = {
      val cnt = localsCount + argsCount + (if (func) 0 else 1)
      if (void) {
        List.fill(cnt)(asm.Operation(Opcodes.POP))
      } else {
        List
          .fill(cnt)(List(asm.Operation(Opcodes.SWAP), asm.Operation(Opcodes.POP)))
          .flatten
      }
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

    val metaMethodMark = if (!func) {
      for {
        methodSign <- tctx.signatures.get(tctx.methodRow(id).signatureIdx)
        methodTpe <- CallsTransation.methodType(methodSign)
        argTpes <- CallsTransation.methodParams(methodSign)
      } yield
        asm.Operation.Meta(
          Meta.MethodSignature(
            name,
            dotnetToVmTpe(methodTpe),
            argTpes.map(tpe => dotnetToVmTpe(tpe.tpe))
          )
        )
    } else {
      None
    }

    for {
      opTranslations <- opTranslationsE
    } yield
      MethodTranslation(
        s"${kind}_$name",
        List(
          OpCodeTranslation(Left(s"$name $kind"),
                            List.empty,
                            metaMethodMark.toList :+ asm.Operation.Label(s"${kind}_$name")),
          OpCodeTranslation(Left(s"$name local vars definition"),
                            List.empty,
                            List.fill(localsCount)(asm.Operation.Push(Data.Primitive.Null))),
          OpCodeTranslation(Left(s"$name $kind body"), List.empty, List.empty)
        )
          ++ opTranslations ++
          List(OpCodeTranslation(Left(s"$name local vars clearing"), List.empty, clear)) ++
          List(
            OpCodeTranslation(Left(s"end of $name $kind"),
                              List.empty,
                              if (func) List(asm.Operation(Opcodes.RET))
                              else List(asm.Operation.Jump(Some("stop"))))),
        functions
      )
  }

  def translateVerbose(methods: List[Method],
                       cilData: CilData,
                       signatures: Map[Long, Signatures.Signature],
                       pdbTables: Option[TablesData] = None): Either[TranslationError, Translation] = {
    val classesWithProgramAttribute = cilData.tables.customAttributeTable.collect {
      case CustomAttributeData(td: TypeDefData,
                               MemberRefData(TypeRefData(_, "Program", "Expload.Pravda"), ".ctor", _)) =>
        td
    }

    if (classesWithProgramAttribute.length != 1) {
      Left(TranslationError(InternalError("You must define exactly one class with [Program] attribute"), None))
    } else {
      val cls = classesWithProgramAttribute(0)
      if (cls.fields.exists(f => FieldsTranslation.isStatic(f.flags))) {
        Left(TranslationError(InternalError("Static fields in program class are forbidden"), None))
      } else {
        val methodsToTypes: Map[Int, TypeDefData] = cilData.tables.methodDefTable.zipWithIndex.flatMap {
          case (m, i) => cilData.tables.typeDefTable.find(_.methods.exists(_ == m)).map(i -> _)
        }.toMap

        val translationCtx = TranslationCtx(signatures, cilData, cls, methodsToTypes, pdbTables)
        translateAllMethods(methods, translationCtx)
      }
    }
  }

  def translateAllMethods(methods: List[Method], tctx: TranslationCtx): Either[TranslationError, Translation] = {

    def isCtor(methodIdx: Int): Boolean = {
      val methodRow = tctx.methodRow(methodIdx)
      methodRow.name == ".ctor" && (methodRow.flags & 0x1800) != 0 // maybe the mask should be different (see 252-nd page in spec)
    }

    def isCctor(methodIdx: Int): Boolean = {
      val methodRow = tctx.methodRow(methodIdx)
      methodRow.name == ".cctor" && (methodRow.flags & 0x1810) != 0 // maybe the mask should be different (see 252-nd page in spec)
    }

    def isMain(methodIdx: Int): Boolean = {
      val methodRow = tctx.methodRow(methodIdx)
      methodRow.name == "Main" && (methodRow.flags & 0x10) != 0
    }

    def isPrivate(methodIdx: Int): Boolean =
      (tctx.methodRow(methodIdx).flags & 0x7) == 0x1

    def isPublic(methodIdx: Int): Boolean =
      (tctx.methodRow(methodIdx).flags & 0x7) == 0x6

    def isStatic(methodIdx: Int): Boolean =
      (tctx.methodRow(methodIdx).flags & 0x10) != 0

    def filterValidateMethods(pred: Int => Boolean,
                              validate: Int => Boolean,
                              onFail: => InnerTranslationError): Either[TranslationError, List[Int]] = {
      val ms = methods.indices.filter(pred).toList
      if (ms.forall(validate)) {
        Right(ms)
      } else {
        Left(TranslationError(onFail, None))
      }
    }
    def filterMethods(pred: Int => Boolean): Either[TranslationError, List[Int]] =
      filterValidateMethods(pred, _ => true, ???)

    val programMethodsFuncsE = for {
      methodsFuncs <- filterValidateMethods(
        i => tctx.isProgramMethod(i) && !isCtor(i) && !isCctor(i) && !isMain(i),
        i => !isStatic(i) && (isPrivate(i) || isPublic(i)),
        InternalError("Only public or private non-static methods are allowed")
      )
      _ <- {
        val names = methodsFuncs.map(i => tctx.methodRow(i).name)
        if (names.toSet.size != names.size) {
          Left(
            TranslationError(InternalError("It's forbidden to have overloaded program's functions or methods"), None))
        } else {
          Right(())
        }
      }
    } yield methodsFuncs

    val programMethodsE = programMethodsFuncsE.map(_.filter(isPublic))
    val programFuncsE = programMethodsFuncsE.map(_.filter(isPrivate))

    val programCtorE = for {
      ctors <- filterValidateMethods(
        i => tctx.isProgramMethod(i) && (isCtor(i) || isCctor(i)),
        i => !isCctor(i) && tctx.methodRow(i).params.isEmpty,
        InternalError("Constructor mustn't take any arguments. Static constructors are forbidden")
      )
      ctor <- {
        if (ctors.length != 1) {
          Left(
            TranslationError(InternalError("It's forbidden to have more than one or zero program's constructors"),
                             None))
        } else {
          Right(ctors.head)
        }
      }
    } yield ctor

    val structEntitiesE = for {
      methods <- filterMethods(i => !tctx.isProgramMethod(i) && !isMain(i))
    } yield methods

    val structFuncsE = structEntitiesE.map(_.filter(i => !isCtor(i) && !isCctor(i) && !isStatic(i)))
    val structStaticFuncsE = structEntitiesE.map(_.filter(i => !isCtor(i) && !isCctor(i) && isStatic(i)))
    val structCtorsE = structEntitiesE.map(_.filter(i => isCtor(i)))
    //val structCctorsE = structEntitiesE.map(_.filter(i => isCctor(i)))

    val jumpToMethodsE: Either[TranslationError, List[Operation]] = for {
      programMethods <- programMethodsE
    } yield {
      programMethods
        .map(i => {
          val name = tctx.cilData.tables.methodDefTable(i).name
          (name,
           List(
             asm.Operation(Opcodes.DUP),
             asm.Operation.Push(Data.Primitive.Utf8(name)),
             asm.Operation(Opcodes.EQ),
             asm.Operation.JumpI(Some(s"method_$name"))
           ))
        })
        .sortBy(_._1)
        .flatMap(_._2)
    }

    val ctorOpsE: Either[TranslationError, MethodTranslation] = for {
      ctor <- programCtorE
      ops <- ctor match {
        case i =>
          val method = methods(i)
          val prefix = List(
            asm.Operation(Opcodes.FROM),
            asm.Operation(Opcodes.PADDR),
            asm.Operation(Opcodes.OWNER),
            asm.Operation(Opcodes.EQ),
            asm.Operation.JumpI(Some("ctor_ok_1")),
            asm.Operation.Push(Data.Primitive.Utf8("Only owner can call the constructor")),
            asm.Operation(Opcodes.THROW),
            asm.Operation.Label("ctor_ok_1"),
            asm.Operation.Push(Data.Primitive.Utf8("init")),
            asm.Operation(Opcodes.SEXIST),
            asm.Operation(Opcodes.NOT),
            asm.Operation.JumpI(Some("ctor_ok_2")),
            asm.Operation.Push(Data.Primitive.Utf8("Program has been already initialized")),
            asm.Operation(Opcodes.THROW),
            asm.Operation.Label("ctor_ok_2"),
            asm.Operation.Push(Data.Primitive.Null),
            asm.Operation.Push(Data.Primitive.Utf8("init")),
            asm.Operation(Opcodes.SPUT),
          )

          translateMethod(
            BranchTransformer.transformBranches(method.opcodes, "ctor"),
            "ctor",
            "method",
            i,
            0,
            MethodExtractors.localVariables(method, tctx.signatures).fold(0)(_.length),
            void = true,
            func = false,
            static = false,
            struct = None,
            tctx.pdbTables.map(_.methodDebugInformationTable(i)),
            tctx
          ).map(res =>
            res.copy(
              opcodes = res.opcodes.head :: OpCodeTranslation(Left("ctor check"), List.empty, prefix) :: res.opcodes.tail))
      }
    } yield ops

    lazy val programMethodsOpsE: Either[TranslationError, List[MethodTranslation]] = for {
      programMethods <- programMethodsE
      opss <- programMethods
        .map(i => {
          val method = methods(i)
          val methodRow = tctx.cilData.tables.methodDefTable(i)

          translateMethod(
            BranchTransformer.transformBranches(method.opcodes, methodRow.name),
            methodRow.name,
            "method",
            i,
            methodRow.params.length,
            MethodExtractors.localVariables(method, tctx.signatures).fold(0)(_.length),
            void = MethodExtractors.isVoid(methodRow, tctx.signatures),
            func = false,
            static = false,
            struct = None,
            tctx.pdbTables.map(_.methodDebugInformationTable(i)),
            tctx
          )
        })
        .sequence
    } yield opss

    lazy val programFuncsOpsE: Either[TranslationError, List[MethodTranslation]] = for {
      programFuncs <- programFuncsE
      opss <- programFuncs
        .map(i => {
          val method = methods(i)
          val methodRow = tctx.cilData.tables.methodDefTable(i)

          translateMethod(
            BranchTransformer.transformBranches(method.opcodes, methodRow.name),
            methodRow.name,
            "func",
            i,
            methodRow.params.length,
            MethodExtractors.localVariables(method, tctx.signatures).fold(0)(_.length),
            void = MethodExtractors.isVoid(methodRow, tctx.signatures),
            func = true,
            static = false,
            struct = None,
            tctx.pdbTables.map(_.methodDebugInformationTable(i)),
            tctx
          )
        })
        .sequence
    } yield opss

    lazy val structFuncsOpsE: Either[TranslationError, List[MethodTranslation]] = for {
      structFuncs <- structFuncsE
      opss <- structFuncs
        .map(i => {
          val method = methods(i)
          val methodRow = tctx.methodRow(i)
          val methodName = CallsTransation.fullMethodName(methodRow.name, tctx.signatures.get(methodRow.signatureIdx))
          val tpe = tctx.methodsToTypes(i)
          val structName = CallsTransation.fullTypeDefName(tpe)
          val name = s"$structName.$methodName"

          translateMethod(
            BranchTransformer.transformBranches(method.opcodes, name),
            name,
            "func",
            i,
            methodRow.params.length,
            MethodExtractors.localVariables(method, tctx.signatures).fold(0)(_.length),
            void = MethodExtractors.isVoid(methodRow, tctx.signatures),
            func = true,
            static = false,
            struct = Some(structName),
            tctx.pdbTables.map(_.methodDebugInformationTable(i)),
            tctx
          )
        })
        .sequence
    } yield opss

    lazy val structStaticFuncOpsE: Either[TranslationError, List[MethodTranslation]] = for {
      structStaticFuncs <- structStaticFuncsE
      opss <- structStaticFuncs
        .map(i => {
          val method = methods(i)
          val methodRow = tctx.methodRow(i)
          val methodName = CallsTransation.fullMethodName(methodRow.name, tctx.signatures.get(methodRow.signatureIdx))
          val tpe = tctx.methodsToTypes(i)
          val structName = CallsTransation.fullTypeDefName(tpe)
          val name = s"$structName.$methodName"

          translateMethod(
            BranchTransformer.transformBranches(method.opcodes, name),
            name,
            "func",
            i,
            methodRow.params.length,
            MethodExtractors.localVariables(method, tctx.signatures).fold(0)(_.length),
            void = MethodExtractors.isVoid(methodRow, tctx.signatures),
            func = true,
            static = true,
            struct = Some(structName),
            tctx.pdbTables.map(_.methodDebugInformationTable(i)),
            tctx
          )
        })
        .sequence
    } yield opss

    lazy val structCtorsOpsE: Either[TranslationError, List[MethodTranslation]] = for {
      structCtors <- structCtorsE
      opss <- structCtors
        .flatMap(i => {
          val method = methods(i)
          val methodRow = tctx.methodRow(i)
          val methodName = CallsTransation.fullMethodName(methodRow.name, tctx.signatures.get(methodRow.signatureIdx))
          val tpe = tctx.methodsToTypes(i)
          val structName = CallsTransation.fullTypeDefName(tpe)
          val name = s"$structName.$methodName"

          List(
            Right(
              MethodTranslation(
                s"vtable_$structName",
                List(
                  OpCodeTranslation(Left(s"$structName vtable initialization"),
                                    List.empty,
                                    Operation.Label(s"vtable_$structName") +:
                                      vtableInit(tpe, tctx) :+
                                      Operation(Opcodes.RET))),
                List.empty
              )),
            translateMethod(
              BranchTransformer.transformBranches(method.opcodes, name),
              name,
              "func",
              i,
              methodRow.params.length,
              MethodExtractors.localVariables(method, tctx.signatures).fold(0)(_.length),
              void = true,
              func = true,
              static = false,
              struct = Some(structName),
              tctx.pdbTables.map(_.methodDebugInformationTable(i)),
              tctx
            )
          )
        })
        .sequence
    } yield opss

    for {
      programMethodsOps <- programMethodsOpsE
      ctorOps <- ctorOpsE
      programFuncOps <- programFuncsOpsE
      structFuncsOps <- structFuncsOpsE
      strucStaticFuncsOps <- structStaticFuncOpsE
      structCtorsOps <- structCtorsOpsE
      jumpToMethods <- jumpToMethodsE
    } yield {
      val methodsOps = ctorOps :: programMethodsOps
      val funcsOps = programFuncOps ++ structFuncsOps ++ strucStaticFuncsOps ++ structCtorsOps
      Translation(
        List(
          asm.Operation(Opcodes.DUP),
          asm.Operation.Push(Data.Primitive.Utf8("ctor")),
          asm.Operation(Opcodes.EQ),
          asm.Operation.JumpI(Some("method_ctor")),
          asm.Operation.Push(Data.Primitive.Utf8("init")),
          asm.Operation(Opcodes.SEXIST),
          asm.Operation.JumpI(Some("methods")),
          asm.Operation.Push(Data.Primitive.Utf8("Program was not initialized")),
          asm.Operation(Opcodes.THROW),
          asm.Operation.Label("methods")
        ) ++ jumpToMethods ++ List(
          asm.Operation.Push(Data.Primitive.Utf8("Wrong method name")),
          asm.Operation(Opcodes.THROW)
        ),
        methodsOps,
        funcsOps,
        distinctFunctions((funcsOps ++ methodsOps).flatMap(_.additionalFunctions)),
        List(asm.Operation.Label("stop"))
      )
    }
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
      t.methods.sortBy(_.name).flatMap(_.opcodes.flatMap(opcodeToAsm)) ++
      t.funcs.sortBy(_.name).flatMap(_.opcodes.flatMap(opcodeToAsm)) ++
      translatorMark("helper functions") ++
      t.helperFunctions.sortBy(_.name).flatMap {
        case OpcodeTranslator.HelperFunction(name, ops) => asm.Operation.Label(name) :: ops
      } ++
      t.finishOps
  }

  def translateAsm(rawMethods: List[Method],
                   cilData: CilData,
                   signatures: Map[Long, Signatures.Signature],
                   pdbTables: Option[TablesData] = None): Either[TranslationError, List[asm.Operation]] =
    translateVerbose(rawMethods, cilData, signatures, pdbTables).map(translationToAsm)
}
