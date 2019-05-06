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
import pravda.dotnet.translation.data.{InternalError, MethodTranslationCtx, InnerTranslationError, UnknownOpcode}
import pravda.dotnet.translation.opcode.OpcodeTranslator.Taken
import pravda.vm.asm.Operation

/** Translator used to ignore initializations of Mappings as fields of class */
object MappingInitializationTranslation extends OpcodeTranslatorOnlyAsm {
  override def asmOps(ops: List[CIL.Op],
                      stackOffsetO: Option[Int],
                      ctx: MethodTranslationCtx): Either[InnerTranslationError, (Taken, List[Operation])] =
    ops.take(2) match {
      case List(NewObj(MemberRefData(TypeSpecData(signIdx), ".ctor", _)), StFld(FieldData(_, _, name, _))) =>
        val res = for {
          parentSig <- ctx.tctx.signatures.get(signIdx)
        } yield {
          if (CallsTranslation.detectMapping(parentSig)) {
            Right(
              (2, List.empty)
            )
          } else {
            Left(UnknownOpcode)
          }
        }

        res.getOrElse(Left(InternalError("Invalid signatures")))
      case _ => Left(UnknownOpcode)
    }
}
