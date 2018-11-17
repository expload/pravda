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

import java.nio.charset.StandardCharsets

import pravda.dotnet.data.TablesData._
import pravda.dotnet.parser.CIL
import pravda.dotnet.parser.CIL._
import pravda.dotnet.parser.Signatures._
import pravda.dotnet.translation.TypeDetectors
import pravda.dotnet.translation.data._
import pravda.vm.{Data, Opcodes, asm}

case object FieldsTranslation extends OneToManyTranslatorOnlyAsm {

  def isStatic(flags: Short): Boolean = (flags & 0x10) != 0

  override def asmOpsOne(op: CIL.Op,
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[InnerTranslationError, List[asm.Operation]] = {

    def loadField(id: Int,
                  name: String,
                  sigIdx: Long,
                  static: Boolean): Either[InnerTranslationError, List[asm.Operation]] = {
      lazy val defaultLoad = Right(
        List(
          pushString(s"p_$name"),
          asm.Operation(Opcodes.SGET)
        ))

      ctx.tctx.typeDefByField(id) match {
        case Some(typeDef) if typeDef == ctx.tctx.mainProgramClass =>
          ctx.tctx.signatures.get(sigIdx) match {
            case Some(FieldSig(tpe)) =>
              tpe match {
                case SigType.Generic(TypeDetectors.Mapping(), _) =>
                  Right(List(pushBytes(name.getBytes(StandardCharsets.UTF_8))))
                case _ =>
                  defaultLoad
              }
            case _ => defaultLoad
          }
        case Some(typeDef) if ctx.tctx.programClasses.contains(typeDef) =>
          Left(InternalError("Reading fields from other [Program] classes is forbidden"))
        case Some(typeDef) =>
          if (static) {
            Right(
              List(
                pushString(s"s_${CallsTranslation.fullTypeDefName(typeDef)}_$name"),
                asm.Operation(Opcodes.SGET)
              ))
          } else {
            Right(List(asm.Operation.StructGet(Some(Data.Primitive.Utf8(name)))))
          }
        case _ => Left(UnknownOpcode)
      }
    }

    def storeField(id: Int,
                   name: String,
                   sigIdx: Long,
                   static: Boolean): Either[InnerTranslationError, List[asm.Operation]] = {
      lazy val defaultStore = Right(
        List(
          pushString(s"p_$name"),
          asm.Operation(Opcodes.SPUT)
        ))

      ctx.tctx.typeDefByField(id) match {
        case Some(typeDef) if typeDef == ctx.tctx.mainProgramClass =>
          ctx.tctx.signatures.get(sigIdx) match {
            case Some(FieldSig(tpe)) =>
              tpe match {
                case SigType.Generic(TypeDetectors.Mapping(), _) =>
                  Right(
                    List(
                      asm.Operation(Opcodes.POP),
                      asm.Operation(Opcodes.POP),
                      pushString("Mapping modification is forbidden"),
                      asm.Operation(Opcodes.THROW)
                    ))
                case _ => defaultStore
              }
            case _ => defaultStore
          }
        case Some(typeDef) if ctx.tctx.programClasses.contains(typeDef) =>
          Left(InternalError("Writing fields from other [Program] classes is forbidden"))
        case Some(typeDef) =>
          if (static) {
            Right(
              List(
                pushString(s"s_${CallsTranslation.fullTypeDefName(typeDef)}_$name"),
                asm.Operation(Opcodes.SPUT)
              ))
          } else {
            Right(List(asm.Operation.StructMut(Some(Data.Primitive.Utf8(name)))))
          }
        case _ => Left(UnknownOpcode)
      }
    }

    op match {
      case LdSFld(FieldData(id, flags, name, sig)) => loadField(id, name, sig, isStatic(flags))
      case LdFld(FieldData(id, flags, name, sig))  => loadField(id, name, sig, isStatic(flags))
      case StSFld(FieldData(id, flags, name, sig)) => storeField(id, name, sig, isStatic(flags))
      case StFld(FieldData(id, flags, name, sig))  => storeField(id, name, sig, isStatic(flags))
      case _                                       => Left(UnknownOpcode)
    }
  }
}
