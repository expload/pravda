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
import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures.MethodRefDefSig
import pravda.dotnet.translation.data.{InternalError, MethodTranslationCtx, InnerTranslationError, UnknownOpcode}
import pravda.vm.{Data, Opcodes}
import pravda.vm.asm.Operation

case object StringTranslation extends OneToManyTranslatorOnlyAsm {

  override def asmOpsOne(op: CIL.Op,
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[InnerTranslationError, List[Operation]] = op match {
    case CallVirt(MemberRefData(TypeRefData(_, "String", "System"), "get_Length", _)) =>
      Right(List(Operation.Orphan(Opcodes.LENGTH)))
    case LdStr(s) =>
      Right(List(Operation.Push(Data.Primitive.Utf8(s))))
    case Call(MemberRefData(TypeRefData(_, "String", "System"), "Concat", signatureIdx)) =>
      val strsCntE = (for {
        sign <- ctx.tctx.signatures.get(signatureIdx)
      } yield
        sign match {
          case MethodRefDefSig(_, _, _, _, _, _, params) => Right(params.length)
          case _                                         => Left(UnknownOpcode)
        }).getOrElse(Left(InternalError("Invalid signatures")))

      for {
        strsCnt <- strsCntE
      } yield List.fill(strsCnt - 1)(List(Operation(Opcodes.SWAP), Operation(Opcodes.CONCAT))).flatten
    case CallVirt(MemberRefData(TypeRefData(_, "String", "System"), "get_Chars", _)) =>
      Right(List(Operation(Opcodes.ARRAY_GET)))
    case CallVirt(MemberRefData(TypeRefData(_, "String", "System"), "Substring", signatureIdx)) => // FIXME more accurate method detection
      Right(
        List(pushInt(2),
             Operation(Opcodes.DUPN),
             Operation(Opcodes.ADD),
             Operation(Opcodes.SWAP),
             Operation(Opcodes.SLICE)))
    case _ => Left(UnknownOpcode)
  }
}
