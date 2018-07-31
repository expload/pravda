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
      ctx: MethodTranslationCtx): Either[TranslationError, List[OpcodeTranslator.HelperFunction]] = op match {
    case CallVirt(MemberRefData(TypeSpecData(parentSigIdx), name, methodSigIdx)) =>
      val res = for {
        parentSig <- ctx.signatures.get(parentSigIdx)
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

  override def deltaOffsetOne(op: CIL.Op, ctx: MethodTranslationCtx): Either[TranslationError, Int] = {
    op match {
      case Call(MethodDefData(_, _, name, signatureIdx, params)) =>
        val void = ctx.signatures.get(signatureIdx).exists(isMethodVoid)
        Right(if (void) -params.length else -params.length + 1)
      case Call(MemberRefData(TypeRefData(_, "Info", "Com.Expload"), "Sender", _))                     => Right(1)
      case Call(MemberRefData(TypeRefData(_, "Info", "Com.Expload"), "Owner", _))                      => Right(0)
      case Call(MemberRefData(TypeRefData(_, "Info", "Com.Expload"), "Balance", _))                    => Right(0)
      case Call(MemberRefData(TypeRefData(_, "Info", "Com.Expload"), "ProgramAddress", _))             => Right(1)
      case Call(MemberRefData(TypeRefData(_, "StdLib", "Com.Expload"), "Ripemd160", _))                => Right(0)
      case Call(MemberRefData(TypeRefData(_, "StdLib", "Com.Expload"), "ValidateEd25519Signature", _)) => Right(-2)
      case Call(MemberRefData(TypeRefData(_, "Object", "System"), ".ctor", _))                         => Right(0)

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
                         ctx: MethodTranslationCtx): Either[TranslationError, List[Operation]] = {

    op match {
      case Call(MethodDefData(_, _, name, _, params))                          => Right(List(Operation.Call(Some(s"func_$name"))))
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
      case CallVirt(MemberRefData(TypeSpecData(parentSigIdx), name, methodSigIdx)) =>
        val res = for {
          parentSig <- ctx.signatures.get(parentSigIdx)
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
                      Operation(Opcodes.SWAP),
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
