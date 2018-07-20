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
import pravda.dotnet.translation.data._
import pravda.vm.{Opcodes, asm}

case object FieldsTranslation extends OneToManyTranslatorOnlyAsm {

  override def asmOpsOne(op: CIL.Op,
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[TranslationError, List[asm.Operation]] = {

    def loadField(name: String, sigIdx: Long): List[asm.Operation] = { // FIXME should process static fields too
      lazy val defaultLoad = List(
        pushString(name),
        asm.Operation(Opcodes.SGET)
      )

      ctx.signatures.get(sigIdx) match {
        case Some(FieldSig(tpe)) =>
          tpe match {
            case SigType.Generic(TypeDetectors.Mapping(), _) =>
              List(pushBytes(name.getBytes(StandardCharsets.UTF_8)))
            case TypeDetectors.Mapping() =>
              List(pushBytes(name.getBytes(StandardCharsets.UTF_8)))
            case _ => defaultLoad
          }
        case _ => defaultLoad
      }
    }

    def storeField(name: String, sigIdx: Long): List[asm.Operation] = { // FIXME should process static fields too
      lazy val defaultStore = List(
        pushString(name),
        asm.Operation(Opcodes.SWAP),
        asm.Operation(Opcodes.SPUT)
      )

      ctx.signatures.get(sigIdx) match {
        case Some(FieldSig(tpe)) =>
          tpe match {
            case TypeDetectors.Mapping() =>
              List(asm.Operation(Opcodes.STOP)) // error user shouldn't modify mappings
            case _ => defaultStore
          }
        case _ => defaultStore
      }
    }

    op match {
      case LdSFld(FieldData(_, name, sig)) => Right(loadField(name, sig))
      case LdFld(FieldData(_, name, sig))  => Right(loadField(name, sig))
      case StSFld(FieldData(_, name, sig)) => Right(storeField(name, sig))
      case StFld(FieldData(_, name, sig))  => Right(storeField(name, sig))
      case _                               => Left(UnknownOpcode)
    }
  }
}
