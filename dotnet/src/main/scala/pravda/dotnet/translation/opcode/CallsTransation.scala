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

package pravda.dotnet.translation.opcode
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._
import pravda.dotnet.translation.TypeDetectors
import pravda.dotnet.translation.data._
import pravda.vm.asm.Operation
import pravda.vm.{Data, Opcodes}

case object CallsTransation extends OneToManyTranslator {

  private val mappingsMethods = Set("get", "getDefault", "exists", "put")

  def detectMapping(sig: Signature): Boolean = {
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

  def fullMethodName(name: String, sig: MethodRefDefSig): String =
    s"${name}_${sig.params.map(_.tpe.mkString).mkString("_")}"

  def fullTypeDefName(typeDefData: TypeDefData): String =
    s"${typeDefData.namespace}.${typeDefData.name}"

  private lazy val getDefaultFunction =
    dupn(2) ++ cast(Data.Type.Bytes) ++ dupn(4) ++
      List(
        Operation.Orphan(Opcodes.CONCAT),
        Operation.Orphan(Opcodes.SEXIST),
        Operation.JumpI(Some("get_default_if")),
        Operation.Orphan(Opcodes.SWAP),
        Operation.Orphan(Opcodes.POP),
        Operation.Orphan(Opcodes.SWAP),
        Operation.Orphan(Opcodes.POP),
        Operation.Orphan(Opcodes.RET),
        Operation.Label("get_default_if"),
        Operation.Orphan(Opcodes.POP)
      ) ++
      cast(Data.Type.Bytes) ++
      List(
        Operation.Orphan(Opcodes.SWAP),
        Operation.Orphan(Opcodes.CONCAT),
        Operation.Orphan(Opcodes.SGET),
        Operation.Orphan(Opcodes.RET)
      )

  override def additionalFunctionsOne(
      op: CIL.Op,
      ctx: MethodTranslationCtx): Either[InnerTranslationError, List[OpcodeTranslator.HelperFunction]] = op match {
    case CallVirt(MemberRefData(TypeSpecData(parentSigIdx), name, methodSigIdx)) =>
      val res = for {
        parentSig <- ctx.tctx.signatures.get(parentSigIdx)
      } yield {
        if (detectMapping(parentSig)) {
          name match {
            case "getDefault" =>
              Right(
                List(
                  OpcodeTranslator.HelperFunction("storage_get_default", getDefaultFunction)
                ))
            case _ => Left(UnknownOpcode)
          }
        } else {
          Left(UnknownOpcode)
        }
      }

      res.getOrElse(Left(InternalError("Invalid signatures")))
    case _ => Left(UnknownOpcode)
  }

  override def deltaOffsetOne(op: CIL.Op, ctx: MethodTranslationCtx): Either[InnerTranslationError, Int] = {
    def callMethodDef(m: MethodDefData): Int = {
      val void = ctx.tctx.signatures.get(m.signatureIdx).exists(isMethodVoid)
      val program = ctx.tctx.isProgramMethod(m)
      -m.params.length + (if (void) 1 else 0) + (if (program) 0 else -1)
    }

    op match {
      case Call(m: MethodDefData)                                                                      => Right(callMethodDef(m))
      case CallVirt(m: MethodDefData)                                                                  => Right(callMethodDef(m))
      case NewObj(m: MethodDefData)                                                                    => Right(callMethodDef(m))
      case Call(MemberRefData(TypeRefData(_, "Info", "Com.Expload"), "Sender", _))                     => Right(1)
      case Call(MemberRefData(TypeRefData(_, "Info", "Com.Expload"), "Owner", _))                      => Right(0)
      case Call(MemberRefData(TypeRefData(_, "Info", "Com.Expload"), "Balance", _))                    => Right(0)
      case Call(MemberRefData(TypeRefData(_, "Info", "Com.Expload"), "ProgramAddress", _))             => Right(1)
      case Call(MemberRefData(TypeRefData(_, "StdLib", "Com.Expload"), "Ripemd160", _))                => Right(0)
      case Call(MemberRefData(TypeRefData(_, "StdLib", "Com.Expload"), "ValidateEd25519Signature", _)) => Right(-2)
      case Call(MemberRefData(TypeRefData(_, "Error", "Com.Expload"), "Throw", _))                     => Right(-1)
      case Call(MemberRefData(TypeRefData(_, "Object", "System"), ".ctor", _))                         => Right(0)

      case CallVirt(MemberRefData(TypeSpecData(parentSigIdx), name, methodSigIdx)) =>
        val res = for {
          parentSig <- ctx.tctx.signatures.get(parentSigIdx)
          methodSig <- ctx.tctx.signatures.get(methodSigIdx)
        } yield {
          lazy val paramsCnt = methodParamsCount(methodSig)
          lazy val offset = if (isMethodVoid(methodSig)) -paramsCnt - 1 else -paramsCnt - 1 + 1

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

    def callMethodDef(m: MethodDefData): Either[InternalError, List[Operation]] = {
      if (ctx.tctx.isProgramMethod(m)) {
        Right(List(Operation.Call(Some(s"func_${m.name}"))))
      } else {
        val res = for {
          sig <- ctx.tctx.signatures.get(m.signatureIdx).collect {
            case m: MethodRefDefSig => m
          }
        } yield {
          val paramsCnt = methodParamsCount(sig)
          Right(
            List(
              Operation.Push(Data.Primitive.Int32(paramsCnt + 1)),
              Operation(Opcodes.DUPN),
              Operation.StructGet(Some(Data.Primitive.Utf8(fullMethodName(m.name, sig)))),
              Operation(Opcodes.CALL)
            ))
        }

        res.getOrElse(Left(InternalError("Wrong signatures")))
      }
    }

    op match {
      case Call(m: MethodDefData)                                              => callMethodDef(m)
      case CallVirt(m: MethodDefData)                                          => callMethodDef(m)
      case NewObj(m: MethodDefData) =>
      case Call(MemberRefData(TypeRefData(_, "Object", "System"), ".ctor", _)) => Right(List.empty)
      case Call(MemberRefData(TypeRefData(_, "Info", "Com.Expload"), "Sender", _)) =>
        Right(List(Operation.Orphan(Opcodes.FROM)))
      case Call(MemberRefData(TypeRefData(_, "Info", "Com.Expload"), "Owner", _)) =>
        Right(List(Operation.Orphan(Opcodes.OWNER)))
      case Call(MemberRefData(TypeRefData(_, "Info", "Com.Expload"), "Balance", _)) =>
        Right(List(Operation.Orphan(Opcodes.BALANCE)))
      case Call(MemberRefData(TypeRefData(_, "Info", "Com.Expload"), "ProgramAddress", _)) =>
        Right(List(Operation.Orphan(Opcodes.PADDR)))
      case Call(MemberRefData(TypeRefData(_, "StdLib", "Com.Expload"), "Ripemd160", _)) =>
        Right(List(Operation.Push(Data.Primitive.Int32(2)), Operation.Orphan(Opcodes.SCALL)))
      case Call(MemberRefData(TypeRefData(_, "StdLib", "Com.Expload"), "ValidateEd25519Signature", _)) =>
        Right(List(Operation.Push(Data.Primitive.Int32(1)), Operation.Orphan(Opcodes.SCALL)))
      case Call(MemberRefData(TypeRefData(_, "Error", "Com.Expload"), "Throw", _)) =>
        Right(List(Operation.Orphan(Opcodes.THROW)))
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

              case "getDefault" => Right(List(Operation.Call(Some("storage_get_default"))))
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
