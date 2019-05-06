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
import pravda.dotnet.data.TablesData.{MemberRefData, TypeRefData}
import pravda.dotnet.parser.{CIL, Signatures}
import pravda.dotnet.parser.CIL._
import pravda.dotnet.parser.Signatures.{MethodRefDefSig, SigType}
import pravda.dotnet.translation.data.{InnerTranslationError, InternalError, MethodTranslationCtx, UnknownOpcode}
import pravda.vm.{Data, Opcodes}
import pravda.vm.asm.Operation

/** Translator that handles `System.String` operations */
case object StringTranslation extends OneToManyTranslatorOnlyAsm {

  override def asmOpsOne(op: CIL.Op,
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[InnerTranslationError, List[Operation]] = op match {
    case CallVirt(MemberRefData(TypeRefData(_, "String", "System"), "get_Length", _)) =>
      Right(List(Operation.Orphan(Opcodes.LENGTH)))
    case LdStr(s) =>
      Right(List(Operation.Push(Data.Primitive.Utf8(s))))
    case Call(MemberRefData(TypeRefData(_, "String", "System"), "Concat", signatureIdx)) =>
      (for {
        sign <- ctx.tctx.signatures.get(signatureIdx)
      } yield
        sign match {
          // `String.Concat` is quite important, because it is used even in `str1 + str2`
          // If we have chain of `+`, e.g `str1 + str2 + str3 + str4 ...` `Concat` with multiple arguments is called
          case MethodRefDefSig(_, _, _, _, _, _, params) if params.forall(_.tpe == SigType.String) =>
            Right(List.fill(params.length - 1)(List(Operation(Opcodes.SWAP), Operation(Opcodes.CONCAT))).flatten)
          case MethodRefDefSig(_, _, _, _, _, _, List(Signatures.Tpe(SigType.Arr(SigType.String, _), _))) =>
            Right(List(Operation.Call(Some("stdlib_concat_all_string"))))
          case _ =>
            Left(UnknownOpcode)
        }).getOrElse(Left(InternalError("Invalid signatures")))
    case CallVirt(MemberRefData(TypeRefData(_, "String", "System"), "get_Chars", _)) =>
      Right(List(Operation(Opcodes.ARRAY_GET)))
    // FIXME more accurate method detection
    // FIXME there is also another form of Substring without 3rd argument
    case CallVirt(MemberRefData(TypeRefData(_, "String", "System"), "Substring", signatureIdx)) =>
      Right(
        List(pushInt(2),
             Operation(Opcodes.DUPN),
             Operation(Opcodes.ADD),
             Operation(Opcodes.SWAP),
             Operation(Opcodes.SLICE)))
    case _ => Left(UnknownOpcode)
  }
}
