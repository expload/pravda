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
import com.google.protobuf.ByteString
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parser.CIL
import pravda.dotnet.parser.CIL._
import pravda.dotnet.translation.data.{MethodTranslationCtx, InnerTranslationError, UnknownOpcode}
import pravda.vm.asm.Operation
import pravda.vm.{Data, Opcodes, asm}

object BytesTranslation extends OneToManyTranslator {

  def deltaOffsetOne(op: CIL.Op, ctx: MethodTranslationCtx): Either[InnerTranslationError, Int] = op match {
    case LdSFld(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), "EMPTY", _))            => Right(1)
    case LdSFld(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), "VOID_ADDRESS", _))     => Right(1)
    case NewObj(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), ".ctor", signatureIdx)) => Right(0)
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), "get_Item", _))       => Right(-1)
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), "Slice", _))          => Right(-2)
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), "Concat", _))         => Right(-1)
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), "Length", _))         => Right(0)
    case _                                                                                       => Left(UnknownOpcode)
  }

  def asmOpsOne(op: CIL.Op,
                stackOffsetO: Option[Int],
                ctx: MethodTranslationCtx): Either[InnerTranslationError, List[asm.Operation]] = op match {
    case LdSFld(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), "EMPTY", _)) =>
      Right(List(Operation.Push(Data.Primitive.Bytes(ByteString.EMPTY))))
    case LdSFld(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), "VOID_ADDRESS", _)) =>
      Right(List(Operation.Push(Data.Primitive.Bytes(ByteString.copyFrom(Array.fill[Byte](32)(0))))))
    case NewObj(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), ".ctor", signatureIdx)) =>
      Right(List(Operation.Call(Some("stdlib_array_to_bytes"))))
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), "get_Item", _)) =>
      Right(List(Operation(Opcodes.ARRAY_GET)))
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), "Slice", _)) =>
      Right(
        List(pushInt(2),
             Operation(Opcodes.DUPN),
             Operation(Opcodes.ADD),
             Operation(Opcodes.SWAP),
             Operation(Opcodes.SLICE))
      )
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), "Concat", _)) =>
      Right(List(Operation(Opcodes.SWAP), Operation(Opcodes.CONCAT)))
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Expload.Pravda"), "Length", _)) =>
      Right(List(Operation(Opcodes.LENGTH)))
    case _ => Left(UnknownOpcode)
  }
}
