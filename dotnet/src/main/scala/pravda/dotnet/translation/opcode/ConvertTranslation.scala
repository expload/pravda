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
import pravda.dotnet.parser.CIL
import pravda.dotnet.parser.CIL._
import pravda.dotnet.translation.data.{MethodTranslationCtx, InnerTranslationError, UnknownOpcode}
import pravda.vm.Data
import pravda.vm.asm.Operation

/** Translator that handles conversions via `System.Convert...` */
object ConvertTranslation extends OneToManyTranslatorOnlyAsm {
  override def asmOpsOne(op: CIL.Op,
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[InnerTranslationError, List[Operation]] = op match {

    case Call(MemberRefData(TypeRefData(_, "Convert", "System"), "ToBoolean", _)) => Right(cast(Data.Type.Boolean))
    case Call(MemberRefData(TypeRefData(_, "Convert", "System"), "ToChar", _))    => Right(cast(Data.Type.Int16))
    case Call(MemberRefData(TypeRefData(_, "Convert", "System"), "ToDouble", _))  => Right(cast(Data.Type.Number))
    case Call(MemberRefData(TypeRefData(_, "Convert", "System"), "ToInt16", _))   => Right(cast(Data.Type.Int16))
    case Call(MemberRefData(TypeRefData(_, "Convert", "System"), "ToInt32", _))   => Right(cast(Data.Type.Int32))
    case Call(MemberRefData(TypeRefData(_, "Convert", "System"), "ToString", _))  => Right(cast(Data.Type.Utf8))
    case Call(MemberRefData(TypeRefData(_, "Convert", "System"), "ToSByte", _))   => Right(cast(Data.Type.Int8))
    case _                                                                        => Left(UnknownOpcode)
  }
}
