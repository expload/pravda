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
import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures._
import pravda.dotnet.translation.TypeDetectors
import pravda.dotnet.translation.data._
import pravda.vm.{Data, Opcodes, asm}

case object FieldsTranslation extends OneToManyTranslatorOnlyAsm {

  def isStatic(flags: Short): Boolean = (flags & 0x10) != 0

  override def asmOpsOne(op: CIL.Op,
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[InnerTranslationError, List[asm.Operation]] = {

    def loadField(name: String, sigIdx: Long, static: Boolean): List[asm.Operation] = {
      lazy val defaultLoadForProgram = List(
        pushString(s"p_$name"),
        asm.Operation(Opcodes.SGET)
      )

      ctx.struct match {
        case Some(structName) =>
          if (static) {
            List(
              pushString(s"s_${structName}_$name"),
              asm.Operation(Opcodes.SGET)
            )
          } else {
            List(asm.Operation.StructGet(Some(Data.Primitive.Utf8(name))))
          }
        case None =>
          ctx.tctx.signatures.get(sigIdx) match {
            case Some(FieldSig(tpe)) =>
              tpe match {
                case SigType.Generic(TypeDetectors.Mapping(), _) =>
                  List(pushBytes(name.getBytes(StandardCharsets.UTF_8)))
                case _ => defaultLoadForProgram
              }
            case _ => defaultLoadForProgram
          }
      }
    }

    def storeField(name: String, sigIdx: Long, static: Boolean): List[asm.Operation] = {
      lazy val defaultStore = List(
        pushString(s"p_$name"),
        asm.Operation(Opcodes.SPUT)
      )

      ctx.struct match {
        case Some(structName) =>
          if (static) {
            List(
              pushString(s"s_${structName}_$name"),
              asm.Operation(Opcodes.SPUT)
            )
          } else {
            List(asm.Operation.StructMut(Some(Data.Primitive.Utf8(name))))
          }
        case None =>
          ctx.tctx.signatures.get(sigIdx) match {
            case Some(FieldSig(tpe)) =>
              tpe match {
                case SigType.Generic(TypeDetectors.Mapping(), _) =>
                  List(
                    asm.Operation(Opcodes.POP),
                    asm.Operation(Opcodes.POP),
                    pushString("Mapping modification is forbidden."),
                    asm.Operation(Opcodes.THROW)
                  )
                case _ => defaultStore
              }
            case _ => defaultStore
          }
      }
    }

    op match {
      case LdSFld(FieldData(flags, name, sig)) => Right(loadField(name, sig, isStatic(flags)))
      case LdFld(FieldData(flags, name, sig))  => Right(loadField(name, sig, isStatic(flags)))
      case StSFld(FieldData(flags, name, sig)) => Right(storeField(name, sig, isStatic(flags)))
      case StFld(FieldData(flags, name, sig))  => Right(storeField(name, sig, isStatic(flags)))
      case _                                   => Left(UnknownOpcode)
    }
  }
}
