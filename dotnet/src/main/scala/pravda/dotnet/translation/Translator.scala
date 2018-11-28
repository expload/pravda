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
import cats.instances.vector._
import cats.instances.either._
import cats.syntax.traverse._
import com.google.protobuf.ByteString
import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parser.CIL
import pravda.dotnet.parser.FileParser.{ParsedDotnetFile, ParsedPdb, ParsedPe}
import pravda.dotnet.parser.Signatures._
import pravda.dotnet.translation.data._
import pravda.dotnet.translation.jumps.{BranchTransformer, StackOffsetResolver}
import pravda.dotnet.translation.opcode.{CallsTranslation, OpcodeTranslator, StdlibAsm}
import pravda.vm.asm.Operation
import pravda.vm.{Data, Meta, Opcodes}

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

object Translator {

  final val CILMark = Meta.Custom("CIL")

  def dotnetToVmTpe(sigType: SigType): Meta.TypeSignature = sigType match {
    case SigType.Void          => Meta.TypeSignature.Null
    case SigType.Boolean       => Meta.TypeSignature.Boolean
    case SigType.I1            => Meta.TypeSignature.Int8
    case SigType.I2            => Meta.TypeSignature.Int16
    case SigType.I4            => Meta.TypeSignature.Int32
    case SigType.I8            => Meta.TypeSignature.BigInt
    case SigType.U1            => Meta.TypeSignature.Uint8
    case SigType.U2            => Meta.TypeSignature.Uint16
    case SigType.U4            => Meta.TypeSignature.Uint32
    case TypeDetectors.Bytes() => Meta.TypeSignature.Bytes
    case SigType.String        => Meta.TypeSignature.Utf8
    // TODO add more types
  }

  private def vtableInit(typeDefData: TypeDefData, tctx: TranslationCtx): List[Operation] = {

    def collectMethodNames(typeDefData: TypeDefData, usedMethods: Vector[String]): Vector[(String, String)] = {
      val methodNames = typeDefData.methods.flatMap {
        case m @ MethodDefData(_, _, flags, name, sigIdx, _) =>
          if (MethodExtractors.isVirtual(m)) {
            Some(CallsTranslation.fullMethodName(name, tctx.signatures.get(sigIdx)))
          } else {
            None
          }
      }

      val notUsedMethods = methodNames.filter(name => !usedMethods.contains(name))
      val notUsedMethodsWithType = notUsedMethods.map((_, CallsTranslation.fullTypeDefName(typeDefData)))

      typeDefData.parent match {
        case typeDef: TypeDefData =>
          notUsedMethodsWithType ++ collectMethodNames(typeDef, usedMethods ++ notUsedMethods)
        case _ => notUsedMethodsWithType
      }
    }

    collectMethodNames(typeDefData, Vector.empty).flatMap {
      case (name, tpe) =>
        List(
          Operation(Opcodes.DUP),
          Operation.PushOffset(s"func_$tpe.$name"),
          Operation.StructMut(Some(Data.Primitive.Utf8(name)))
        )
    }.toList
  }

  private def initStructFields(typeDefData: TypeDefData, tctx: TranslationCtx): List[Operation] = {
    def initField(f: FieldData): List[Operation] = {
      val defaultValue = (for {
        sig <- tctx.signatures.get(f.signatureIdx)
      } yield {
        sig match {
          case FieldSig(tpe) =>
            tpe match {
              case SigType.Boolean       => Data.Primitive.Bool.False
              case SigType.I1            => Data.Primitive.Int8(0)
              case SigType.I2            => Data.Primitive.Int16(0)
              case SigType.I4            => Data.Primitive.Int32(0)
              case SigType.U1            => Data.Primitive.Uint8(0)
              case SigType.U2            => Data.Primitive.Uint16(0)
              case SigType.U4            => Data.Primitive.Uint32(0)
              case SigType.R4            => Data.Primitive.Number(0.0)
              case SigType.R8            => Data.Primitive.Number(0.0)
              case TypeDetectors.Bytes() => Data.Primitive.Bytes(ByteString.EMPTY)
              case SigType.String        => Data.Primitive.Utf8("")
              case _                     => Data.Primitive.Null
            }
          case _ => Data.Primitive.Null
        }
      }).getOrElse(Data.Primitive.Null)

      List(Operation(Opcodes.DUP), Operation.Push(defaultValue), Operation.StructMut(Some(Data.Primitive.Utf8(f.name))))
    }

    typeDefData.fields.toList.flatMap(initField)
  }

  private def eliminateDeadFuncs(methods: List[MethodTranslation],
                                 funcs: List[MethodTranslation]): List[MethodTranslation] = {

    val nameToMethod = (methods ++ funcs).map(m => m.label -> m).toMap

    @tailrec
    def allNames(used: Set[String], toCollect: Set[String]): Set[String] = {
      val notUsed = toCollect.flatMap(name =>
        nameToMethod.get(name) match {
          case Some(m) =>
            m.opcodes.flatMap(_.asmOps).collect {
              case Operation.Call(Some(newName)) if !used.contains(newName) => newName
            }
          case None => List.empty
      })

      if (notUsed.nonEmpty) {
        allNames(used ++ notUsed, notUsed)
      } else {
        used
      }
    }

    val init = (methods ++ funcs.filter(_.forceAdd)).map(_.label).toSet
    val all = allNames(init, init)
    funcs.filter(f => all.contains(f.label))
  }

  private def inspectProgramTypeDef(typeDef: TypeDefData, tctx: TranslationCtx): Either[TranslationError, Unit] = {
    lazy val staticFields = {
      val staticFieldO = typeDef.fields.find(f => FieldExtractors.isStatic(f.flags))
      staticFieldO match {
        case Some(f) =>
          Left(
            TranslationError(InternalError(s"[Program] must not contain static fields: " +
                               s"${CallsTranslation.fullTypeDefName(typeDef)} contains static ${f.name}"),
                             None))
        case None =>
          Right(())
      }
    }

    lazy val privateMappings = typeDef.fields.map { f =>
      val isMapping = for {
        parentSig <- tctx.signatures.get(f.signatureIdx)
      } yield CallsTranslation.detectMapping(parentSig)

      if (isMapping.getOrElse(false) && !FieldExtractors.isPrivate(f)) {
        Left(
          TranslationError(
            InternalError(
              s"All Mapping must be private: ${f.name} in ${CallsTranslation.fullTypeDefName(typeDef)} is not private"),
            None))
      } else {
        Right(())
      }
    }.sequence

    for {
      _ <- staticFields
      _ <- privateMappings
    } yield ()
  }

  private def inspectStructTypeDef(typeDef: TypeDefData, tctx: TranslationCtx): Either[TranslationError, Unit] = {
    typeDef.fields
      .map { f =>
        val isMapping = for {
          parentSig <- tctx.signatures.get(f.signatureIdx)
        } yield CallsTranslation.detectMapping(parentSig)

        if (isMapping.getOrElse(false)) {
          Left(TranslationError(InternalError(s"User defined classes must not contain Mappings: ${CallsTranslation
            .fullTypeDefName(typeDef)} contains ${f.name}"), None))
        } else {
          Right(())
        }
      }
      .sequence
      .map(_ => ())
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
                              forceAdd: Boolean,
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
        List.fill(cnt)(Operation(Opcodes.POP))
      } else {
        List
          .fill(cnt)(List(Operation(Opcodes.SWAP), Operation(Opcodes.POP)))
          .flatten
      }
    }

    val metaMethodMark = if (!func) {
      for {
        methodSign <- tctx.signatures.get(tctx.methodRow(id).signatureIdx)
        methodTpe <- MethodExtractors.methodType(methodSign)
        argTpes <- MethodExtractors.methodParams(methodSign)
      } yield
        Operation.Meta(
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
        kind,
        name,
        forceAdd,
        List(
          OpCodeTranslation(List.empty, metaMethodMark.toList),
          OpCodeTranslation(List.empty, List.fill(localsCount)(Operation.Push(Data.Primitive.Null)))
        )
          ++ opTranslations ++
          List(OpCodeTranslation(List.empty, Operation.Label(s"${name}_lvc") :: clear)) ++
          List(
            OpCodeTranslation(List.empty,
                              if (func) List(Operation(Opcodes.RET))
                              else List(Operation.Jump(Some("stop")))))
      )
  }

  def translateVerbose(files: List[ParsedDotnetFile],
                       mainClass: Option[String]): Either[TranslationError, Translation] = {
    val programClasses = files.flatMap(f =>
      f.parsedPe.cilData.tables.customAttributeTable.collect {
        case CustomAttributeData(td: TypeDefData,
                                 MemberRefData(TypeRefData(_, "Program", "Expload.Pravda"), ".ctor", _)) =>
          td
    })

    val mainProgramClassE = mainClass match {
      case None =>
        if (programClasses.length != 1) {
          Left(TranslationError(InternalError("You must define exactly one class with [Program] attribute"), None))
        } else {
          Right(programClasses.head)
        }
      case Some(name) =>
        programClasses
          .find(cls => CallsTranslation.fullTypeDefName(cls) == name)
          .toRight(TranslationError(InternalError(s"Unable to find $name class with [Program] attribute"), None))
    }

    val methodParents: Map[MethodDefData, TypeDefData] = files.flatMap { f =>
      val tables = f.parsedPe.cilData.tables
      val signatures = f.parsedPe.signatures

      tables.methodDefTable.flatMap { m =>
        val parent = tables.typeDefTable.find(_.methods.exists(_ == m))
        parent.map(p => m -> p)
      }
    }.toMap

    val methodDefs: Map[String, MethodDefData] = files.flatMap { f =>
      val tables = f.parsedPe.cilData.tables
      val signatures = f.parsedPe.signatures

      tables.methodDefTable.flatMap { m =>
        val parent = methodParents.get(m)
        parent.map(p => CallsTranslation.fullTypeMethodName(p, m.name, signatures.get(m.signatureIdx)) -> m)
      }
    }.toMap

    mainProgramClassE.flatMap { mainCls =>
      val translations = for {
        (f, parents) <- files.zip(methodParents)
      } yield {
        val tables = f.parsedPe.cilData.tables
        val structs = tables.typeDefTable.filterNot(programClasses.contains).toList

        val translationCtx = TranslationCtx(f.parsedPe.signatures,
                                            f.parsedPe.cilData,
                                            mainCls,
                                            programClasses,
                                            structs,
                                            methodDefs,
                                            methodParents,
                                            f.parsedPdb.map(_.tablesData))

        val methods = f.parsedPe.methods
          .filter { m => methodParents.get(m).forall(_.namespace != "Expload.Pravda") }
          .map(_._1)

        for {
          _ <- programClasses.map(td => inspectProgramTypeDef(td, translationCtx)).sequence
          _ <- structs.map(td => inspectStructTypeDef(td, translationCtx)).sequence
          res <- translateAllMethods(methods, translationCtx)
        } yield res
      }

      val file =
        (translations.sequence: Either[TranslationError, List[FileTranslation]]).map(_.reduce[FileTranslation] {
          case (FileTranslation(methods1, funcs1), FileTranslation(methods2, funcs2)) =>
            FileTranslation(methods1 ++ methods2, funcs1 ++ funcs2)
        })

      file.map(Translation(_, CallsTranslation.fullTypeDefName(mainCls).replace(".", "")))
    }
  }

  def translateAllMethods(methods: List[Method], tctx: TranslationCtx): Either[TranslationError, FileTranslation] = {

    def withMethodTable(isFunc: MethodDefData => Boolean): Int => Boolean =
      i => isFunc(tctx.methodRow(i))

    val isCtor = withMethodTable(MethodExtractors.isCtor)
    val isCctor = withMethodTable(MethodExtractors.isCctor)
    val isMain = withMethodTable(MethodExtractors.isMain)
    val isPrivate = withMethodTable(MethodExtractors.isPrivate)
    val isPublic = withMethodTable(MethodExtractors.isPublic)
    val isStatic = withMethodTable(MethodExtractors.isStatic)

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
        i => tctx.isMainProgramMethod(i) && !isCtor(i) && !isCctor(i) && !isMain(i) && !isStatic(i),
        i => isPrivate(i) || isPublic(i),
        InternalError("Only public or private methods are allowed")
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
        i => tctx.isMainProgramMethod(i) && (isCtor(i) || isCctor(i)),
        i => !isCctor(i) && tctx.methodRow(i).params.isEmpty,
        InternalError("Constructor mustn't take any arguments. Static constructors are forbidden")
      )
      ctor <- {
        if (ctors.length > 1) {
          Left(TranslationError(InternalError("It's forbidden to have more than one program's constructors"), None))
        } else {
          Right(ctors.headOption)
        }
      }
    } yield ctor

    val structEntitiesE = for {
      methods <- filterMethods(
        i => (!tctx.isProgramMethod(i) || (tctx.isProgramMethod(i) && isStatic(i))) && !isMain(i))
    } yield methods

    val structFuncsE = structEntitiesE.map(_.filter(i => !isCtor(i) && !isCctor(i) && !isStatic(i)))
    val structStaticFuncsE = structEntitiesE.map(_.filter(i => !isCtor(i) && !isCctor(i) && isStatic(i)))
    val structCtorsE = structEntitiesE.map(_.filter(i => isCtor(i)))
    //val structCctorsE = structEntitiesE.map(_.filter(i => isCctor(i)))

    val ctorOpsE: Either[TranslationError, List[MethodTranslation]] = for {
      ctor <- programCtorE
      ops <- ctor match {
        case Some(i) =>
          val method = methods(i)
          val prefix = List(
            Operation.Push(Data.Primitive.Utf8("init")),
            Operation(Opcodes.SEXIST),
            Operation(Opcodes.NOT),
            Operation.JumpI(Some("ctor_ok")),
            Operation.Push(Data.Primitive.Utf8("Program has been already initialized")),
            Operation(Opcodes.THROW),
            Operation.Label("ctor_ok"),
            Operation.Push(Data.Primitive.Null),
            Operation.Push(Data.Primitive.Utf8("init")),
            Operation(Opcodes.SPUT)
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
            forceAdd = true,
            tctx.pdbTables.map(_.methodDebugInformationTable(i)),
            tctx
          ).map(res =>
              res.copy(opcodes = res.opcodes.head :: OpCodeTranslation(List.empty, prefix) :: res.opcodes.tail))
            .map(List(_))
        case None =>
          Right(List.empty)
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
            forceAdd = true,
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
            forceAdd = false,
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
          val methodName = CallsTranslation.fullMethodName(methodRow.name, tctx.signatures.get(methodRow.signatureIdx))
          val tpe = tctx.methodParents(i).get
          val structName = CallsTranslation.fullTypeDefName(tpe)
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
            forceAdd = true,
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
          val methodName = CallsTranslation.fullMethodName(methodRow.name, tctx.signatures.get(methodRow.signatureIdx))
          val tpe = tctx.methodParents(i).get
          val structName = CallsTranslation.fullTypeDefName(tpe)
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
            forceAdd = false,
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
          val methodName = CallsTranslation.fullMethodName(methodRow.name, tctx.signatures.get(methodRow.signatureIdx))
          val tpe = tctx.methodParents(i).get
          val structName = CallsTranslation.fullTypeDefName(tpe)
          val name = s"$structName.$methodName"

          List(
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
              forceAdd = true,
              tctx.pdbTables.map(_.methodDebugInformationTable(i)),
              tctx
            )
          )
        })
        .sequence
    } yield opss

    lazy val structCtorHelpers: List[MethodTranslation] = tctx.structs.flatMap { s =>
      List(
        MethodTranslation(
          "vtable",
          CallsTranslation.fullTypeDefName(s),
          forceAdd = false,
          List(
            OpCodeTranslation(List.empty,
                              vtableInit(s, tctx) :+
                                Operation(Opcodes.RET)))
        ),
        MethodTranslation(
          "default_fields",
          CallsTranslation.fullTypeDefName(s),
          forceAdd = false,
          List(
            OpCodeTranslation(List.empty,
                              initStructFields(s, tctx) :+
                                Operation(Opcodes.RET)))
        )
      )
    }

    for {
      programMethodsOps <- programMethodsOpsE
      ctorOps <- ctorOpsE
      programFuncOps <- programFuncsOpsE
      structFuncsOps <- structFuncsOpsE
      strucStaticFuncsOps <- structStaticFuncOpsE
      structCtorsOps <- structCtorsOpsE
    } yield {
      val methodsOps = ctorOps ++ programMethodsOps
      val funcsOps = programFuncOps ++ structFuncsOps ++ strucStaticFuncsOps ++ structCtorsOps ++ structCtorHelpers
      FileTranslation(methodsOps, eliminateDeadFuncs(methodsOps, funcsOps ++ StdlibAsm.stdlibFuncs))
    }
  }

  def translationToAsm(t: Translation): List[Operation] = {

    val jumpToMethods = t.file.methods
      .filter(_.name != "ctor")
      .sortBy(_.name)
      .flatMap(
        m =>
          List(
            Operation(Opcodes.DUP),
            Operation.Push(Data.Primitive.Utf8(m.name)),
            Operation(Opcodes.EQ),
            Operation.JumpI(Some(s"${m.kind}_${m.name}"))
        ))

    val ctorCheck = List(
      Operation(Opcodes.DUP),
      Operation.Push(Data.Primitive.Utf8("ctor")),
      Operation(Opcodes.EQ),
      Operation.JumpI(Some("method_ctor")),
      Operation.Push(Data.Primitive.Utf8("init")),
      Operation(Opcodes.SEXIST),
      Operation.JumpI(Some("methods")),
      Operation.Push(Data.Primitive.Utf8("Program was not initialized")),
      Operation(Opcodes.THROW)
    )

    val prefix = Operation.Meta(CILMark) :: Operation.Meta(Meta.ProgramName(t.programName)) :: ctorCheck ++
      List(Operation.Label("methods")) ++ jumpToMethods ++ List(
      Operation.Push(Data.Primitive.Utf8("Wrong method name")),
      Operation(Opcodes.THROW)
    )

    def opcodeToAsm(opT: OpCodeTranslation): List[Operation] = {
      val sourceMarks = opT.sourceMarks.map(Operation.Meta(_))
      sourceMarks ++ opT.asmOps
    }

    prefix ++
      t.file.methods.sortBy(_.name).flatMap(m => Operation.Label(m.label) :: m.opcodes.flatMap(opcodeToAsm)) ++
      t.file.funcs.sortBy(_.label).flatMap(f => Operation.Label(f.label) :: f.opcodes.flatMap(opcodeToAsm)) ++
      List(Operation.Label("stop"))
  }

  def translateAsm(parsedDotnetFiles: List[ParsedDotnetFile],
                   mainClass: Option[String]): Either[TranslationError, List[Operation]] =
    translateVerbose(parsedDotnetFiles, mainClass).map(translationToAsm)

  def translateAsm(parsedPe: ParsedPe,
                   parsedPdb: Option[ParsedPdb] = None,
                   mainClass: Option[String] = None): Either[TranslationError, List[Operation]] =
    translateAsm(List(ParsedDotnetFile(parsedPe, parsedPdb)), mainClass)
}
