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

package opcode

import pravda.dotnet.data.TablesData._
import pravda.dotnet.parser.CIL
import pravda.dotnet.parser.CIL._
import pravda.dotnet.parser.Signatures.SigType._
import pravda.dotnet.parser.Signatures._
import pravda.dotnet.translation.data._
import pravda.vm.asm.Operation
import pravda.vm.{Data, Opcodes}

case object CallsTranslation extends OneToManyTranslator {

  private val mappingsMethods = Set("get", "getDefault", "exists", "put")

  def detectMapping(sig: Signature): Boolean = {
    sig match {
      case TypeSig(Tpe(Generic(TypeDetectors.Mapping(), _), _)) => true
      case FieldSig(Generic(TypeDetectors.Mapping(), _))        => true
      case _                                                    => false
    }
  }

  def fullMethodName(name: String, sigO: Option[Signature]): String = {
    val normalizedName = if (name == ".ctor" || name == ".cctor") name.drop(1) else name
    val sigParams = sigO.collect { case m: MethodRefDefSig => m.params }.getOrElse(List.empty)
    if (sigParams.nonEmpty) {
      s"${normalizedName}_${sigParams.map(_.tpe.mkString).mkString("_")}"
    } else {
      normalizedName
    }
  }

  def fullTypeName(namespace: String, name: String): String =
    if (namespace.nonEmpty) {
      s"$namespace.$name"
    } else {
      name
    }

  def fullTypeDefName(typeDefData: TypeDefData): String =
    fullTypeName(typeDefData.namespace, typeDefData.name)

  override def deltaOffsetOne(op: CIL.Op, ctx: MethodTranslationCtx): Either[InnerTranslationError, Int] = {
    def callMethodDef(m: MethodDefData): Int = {
      val void = MethodExtractors.isVoid(m, ctx.tctx.signatures)
      val program = ctx.tctx.isMainProgramMethod(m)
      -m.params.length + (if (void) 0 else 1) + (if (program || ctx.static) 0 else -1)
    }

    def callMethodRef(signatureIdx: Long): Int = {
      val sig = ctx.tctx.signatures.get(signatureIdx)
      val void = sig.exists(MethodExtractors.isVoid)
      val paramsLen = sig.map(MethodExtractors.methodParamsCount).getOrElse(0)
      -paramsLen - 1 + (if (void) 0 else 1)
    }

    op match {
      case Call(m: MethodDefData)     => Right(callMethodDef(m))
      case CallVirt(m: MethodDefData) => Right(callMethodDef(m))
      case NewObj(m: MethodDefData)   => Right(callMethodDef(m) + 2)

      case CallVirt(m @ MemberRefData(TypeRefData(_, name, namespace), methodName, signatureIdx))
          if ctx.tctx.programClasses.exists(cls => fullTypeDefName(cls) == fullTypeName(namespace, name)) =>
        Right(callMethodRef(signatureIdx))

      case Call(MemberRefData(TypeRefData(_, "Object", "System"), ".ctor", _)) =>
        if (ctx.struct.isDefined) Right(-1) else Right(0)
      case Call(MemberRefData(TypeRefData(_, "Actions", "Expload.Pravda"), "Transfer", _))                => Right(-2)
      case Call(MemberRefData(TypeRefData(_, "Actions", "Expload.Pravda"), "TransferFromProgram", _))     => Right(-2)
      case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "Sender", _))                     => Right(1)
      case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "Balance", _))                    => Right(0)
      case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "ProgramAddress", _))             => Right(1)
      case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "Callers", _))                    => Right(1)
      case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "Height", _))                     => Right(1)
      case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "LastBlockHash", _))              => Right(1)
      case Call(MemberRefData(TypeRefData(_, "StdLib", "Expload.Pravda"), "Ripemd160", _))                => Right(0)
      case Call(MemberRefData(TypeRefData(_, "StdLib", "Expload.Pravda"), "ValidateEd25519Signature", _)) => Right(-2)
      case Call(MemberRefData(TypeRefData(_, "StdLib", "Expload.Pravda"), "HexToBytes", _))               => Right(0)
      case Call(MemberRefData(TypeRefData(_, "Error", "Expload.Pravda"), "Throw", _))                     => Right(-1)
      case Call(MethodSpecData(MemberRefData(TypeRefData(_, "ProgramHelper", "Expload.Pravda"), "Program", _), _)) =>
        Right(0)
      case Call(MethodSpecData(MemberRefData(TypeRefData(_, "Log", "Expload.Pravda"), "Event", _), _)) =>
        Right(-2)

      case CallVirt(MemberRefData(TypeSpecData(parentSigIdx), name, methodSigIdx)) =>
        val res = for {
          parentSig <- ctx.tctx.signatures.get(parentSigIdx)
          methodSig <- ctx.tctx.signatures.get(methodSigIdx)
        } yield {
          lazy val paramsCnt = MethodExtractors.methodParamsCount(methodSig)
          lazy val offset = if (MethodExtractors.isVoid(methodSig)) -paramsCnt - 1 else -paramsCnt - 1 + 1

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
                         ctx: MethodTranslationCtx): Either[InnerTranslationError, List[Operation]] = {

    def clearObject(m: MethodDefData): List[Operation] = {
      val void = MethodExtractors.isVoid(m, ctx.tctx.signatures)
      val static = MethodExtractors.isStatic(m)
      if (static) {
        List.empty
      } else if (void) {
        List(Operation(Opcodes.POP))
      } else {
        List(Operation(Opcodes.SWAP), Operation(Opcodes.POP))
      }
    }

    def callFunc(m: MethodDefData): Either[InnerTranslationError, List[Operation]] =
      if (ctx.tctx.isMainProgramMethod(m)) {
        Right(List(Operation.Call(Some(s"func_${m.name}"))))
      } else {
        val tpeO = ctx.tctx.tpeByMethodDef(m)
        tpeO match {
          case Some(tpe) =>
            Right(
              Operation.Call(
                Some(
                  s"func_${fullTypeDefName(tpe)}.${fullMethodName(m.name, ctx.tctx.signatures.get(m.signatureIdx))}"
                )) +: clearObject(m)
            )
          case None =>
            Left(UnknownOpcode)
        }
      }

    op match {
      case Call(m: MethodDefData) => callFunc(m)
      case CallVirt(m: MethodDefData) =>
        if (!ctx.tctx.isMainProgramMethod(m) && MethodExtractors.isVirtual(m)) {
          val res = for {
            sig <- ctx.tctx.signatures.get(m.signatureIdx).collect {
              case m: MethodRefDefSig => m
            }
          } yield {
            val paramsCnt = MethodExtractors.methodParamsCount(sig)
            Right(
              List(
                Operation.Push(Data.Primitive.Int32(paramsCnt + 1)),
                Operation(Opcodes.DUPN),
                Operation.StructGet(Some(Data.Primitive.Utf8(fullMethodName(m.name, Some(sig))))),
                Operation.Call(None),
              ) ++ clearObject(m)
            )
          }

          res.getOrElse(Left(InternalError("Wrong signatures")))
        } else {
          callFunc(m)
        }
      case NewObj(m: MethodDefData) =>
        val tpeO = ctx.tctx.tpeByMethodDef(m)
        tpeO match {
          case Some(tpe) =>
            Right(
              List(
                Operation.New(Data.Struct.empty),
                Operation.Call(Some(s"vtable_${fullTypeDefName(tpe)}")),
                Operation.Call(Some(s"default_fields_${fullTypeDefName(tpe)}"))
              ) ++
                m.params.length
                  .to(1, -1)
                  .toList
                  .flatMap(
                    i =>
                      List(
                        Operation.Push(Data.Primitive.Int32(i + 1)),
                        Operation(Opcodes.SWAPN)
                    )
                  ) // move object to 0-th arg
                :+ Operation.Call(
                  Some(
                    s"func_${fullTypeDefName(tpe)}.${fullMethodName(m.name, ctx.tctx.signatures.get(m.signatureIdx))}"
                  )
                )
            )

          case None => Left(UnknownOpcode)
        }
      case CallVirt(m @ MemberRefData(TypeRefData(_, name, namespace), methodName, _))
          if ctx.tctx.programClasses.exists(cls => fullTypeDefName(cls) == fullTypeName(namespace, name)) =>
        val paramsLen = MethodExtractors.methodParamsCount(m, ctx.tctx.signatures)
        val swapAddress = (2 to (paramsLen + 1))
          .flatMap(i => List(Operation.Push(Data.Primitive.Int32(i)), Operation(Opcodes.SWAPN)))
          .toList
        Right(
          swapAddress ++ List(Operation.Push(Data.Primitive.Utf8(methodName)),
                              Operation(Opcodes.SWAP),
                              Operation.Push(Data.Primitive.Int32(paramsLen + 1)),
                              Operation(Opcodes.PCALL)))
      case Call(MemberRefData(TypeRefData(_, "Object", "System"), ".ctor", _)) =>
        if (ctx.struct.isDefined) Right(List(Operation(Opcodes.POP))) else Right(List.empty)
      case Call(MemberRefData(TypeRefData(_, "Actions", "Expload.Pravda"), "Transfer", _)) =>
        Right(List(Operation(Opcodes.TRANSFER)))
      case Call(MemberRefData(TypeRefData(_, "Actions", "Expload.Pravda"), "TransferFromProgram", _)) =>
        Right(List(Operation(Opcodes.PTRANSFER)))
      case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "Sender", _)) =>
        Right(List(Operation(Opcodes.FROM)))
      case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "Balance", _)) =>
        Right(List(Operation(Opcodes.BALANCE)))
      case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "ProgramAddress", _)) =>
        Right(List(Operation(Opcodes.PADDR)))
      case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "Callers", _)) =>
        Right(List(Operation(Opcodes.CALLERS)))
      case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "Height", _)) =>
        Right(List(Operation(Opcodes.HEIGHT)))
      case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "LastBlockHash", _)) =>
        Right(List(Operation(Opcodes.HASH)))
      case Call(MemberRefData(TypeRefData(_, "StdLib", "Expload.Pravda"), "Ripemd160", _)) =>
        Right(List(Operation.Push(Data.Primitive.Int32(2)), Operation(Opcodes.SCALL)))
      case Call(MemberRefData(TypeRefData(_, "StdLib", "Expload.Pravda"), "HexToBytes", _)) =>
        Right(List(Operation.Push(Data.Primitive.Int32(4)), Operation(Opcodes.SCALL)))
      case Call(MemberRefData(TypeRefData(_, "StdLib", "Expload.Pravda"), "ValidateEd25519Signature", _)) =>
        Right(List(Operation.Push(Data.Primitive.Int32(1)), Operation(Opcodes.SCALL)))
      case Call(MemberRefData(TypeRefData(_, "Error", "Expload.Pravda"), "Throw", _)) =>
        Right(List(Operation(Opcodes.THROW)))
      case Call(MethodSpecData(MemberRefData(TypeRefData(_, "Log", "Expload.Pravda"), "Event", _), _)) =>
        Right(List(Operation(Opcodes.SWAP), Operation(Opcodes.EVENT)))
      case Call(MethodSpecData(MemberRefData(TypeRefData(_, "ProgramHelper", "Expload.Pravda"), "Program", _), _)) =>
        Right(List())
      case CallVirt(MemberRefData(TypeSpecData(parentSigIdx), name, methodSigIdx)) =>
        val res = for {
          parentSig <- ctx.tctx.signatures.get(parentSigIdx)
        } yield {
          if (detectMapping(parentSig)) {
            name match {
              case "get" =>
                Right(
                  cast(Data.Type.Bytes) ++ List(Operation(Opcodes.SWAP),
                                                Operation(Opcodes.CONCAT),
                                                Operation(Opcodes.SGET)))

              case "getDefault" => Right(List(Operation.Call(Some("stdlib_storage_get_default"))))
              case "exists" =>
                Right(
                  cast(Data.Type.Bytes) ++ List(Operation(Opcodes.SWAP),
                                                Operation(Opcodes.CONCAT),
                                                Operation(Opcodes.SEXIST)) ++ cast(Data.Type.Int32))
              case "put" =>
                Right(
                  dupn(2) ++ cast(Data.Type.Bytes) ++ dupn(4) ++
                    List(
                      Operation(Opcodes.CONCAT),
                      Operation(Opcodes.SPUT),
                      Operation(Opcodes.POP),
                      Operation(Opcodes.POP)
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
