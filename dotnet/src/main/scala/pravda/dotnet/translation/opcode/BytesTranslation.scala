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
import pravda.dotnet.parsers.CIL
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.translation.data.{MethodTranslationCtx, InnerTranslationError, UnknownOpcode}
import pravda.vm.asm.Operation
import pravda.vm.{Data, Opcodes, asm}

object BytesTranslation extends OneToManyTranslator {

  override def additionalFunctionsOne(
      op: CIL.Op,
      ctx: MethodTranslationCtx): Either[InnerTranslationError, List[OpcodeTranslator.HelperFunction]] = op match {
    case NewObj(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), ".ctor", signatureIdx)) =>
      Right(
        List(
          OpcodeTranslator.HelperFunction(
            "array_to_bytes",
            List(
              Operation(Opcodes.DUP),
              Operation(Opcodes.LENGTH),
              Operation.Push(Data.Primitive.Bytes(ByteString.EMPTY)),
              pushInt(0),
              Operation.Label("array_to_bytes_loop"),
              pushInt(4),
              Operation(Opcodes.DUPN),
              pushInt(2),
              Operation(Opcodes.DUPN),
              Operation(Opcodes.ARRAY_GET),
              pushType(Data.Type.Bytes),
              Operation(Opcodes.CAST),
              pushInt(3),
              Operation(Opcodes.DUPN),
              Operation(Opcodes.CONCAT),
              pushInt(3),
              Operation(Opcodes.SWAPN),
              Operation(Opcodes.POP),
              pushInt(1),
              Operation(Opcodes.ADD),
              Operation(Opcodes.DUP),
              pushInt(4),
              Operation(Opcodes.DUPN),
              Operation(Opcodes.GT),
              Operation.JumpI(Some("array_to_bytes_loop")),
              Operation(Opcodes.POP),
              Operation(Opcodes.SWAP),
              Operation(Opcodes.POP),
              Operation(Opcodes.SWAP),
              Operation(Opcodes.POP),
              Operation(Opcodes.RET)
            )
          )))
    case _ => Left(UnknownOpcode)
  }

  def deltaOffsetOne(op: CIL.Op, ctx: MethodTranslationCtx): Either[InnerTranslationError, Int] = op match {
    case LdSFld(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "EMPTY", _))            => Right(1)
    case LdSFld(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "VOID_ADDRESS", _))     => Right(1)
    case NewObj(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), ".ctor", signatureIdx)) => Right(0)
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "get_Item", _))       => Right(-1)
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "Slice", _))          => Right(-2)
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "Concat", _))         => Right(-1)
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "Length", _))         => Right(0)
    case _                                                                                    => Left(UnknownOpcode)
  }

  def asmOpsOne(op: CIL.Op,
                stackOffsetO: Option[Int],
                ctx: MethodTranslationCtx): Either[InnerTranslationError, List[asm.Operation]] = op match {
    case LdSFld(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "EMPTY", _)) =>
      Right(List(Operation.Push(Data.Primitive.Bytes(ByteString.EMPTY))))
    case LdSFld(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "VOID_ADDRESS", _)) =>
      Right(List(Operation.Push(Data.Primitive.Bytes(ByteString.copyFrom(Array.fill[Byte](32)(0))))))
    case NewObj(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), ".ctor", signatureIdx)) =>
      Right(List(Operation.Call(Some("array_to_bytes"))))
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "get_Item", _)) =>
      Right(List(Operation(Opcodes.ARRAY_GET)))
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "Slice", _)) =>
      Right(
        List(pushInt(2),
             Operation(Opcodes.DUPN),
             Operation(Opcodes.ADD),
             Operation(Opcodes.SWAP),
             Operation(Opcodes.SLICE))
      )
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "Concat", _)) =>
      Right(List(Operation(Opcodes.SWAP), Operation(Opcodes.CONCAT)))
    case CallVirt(MemberRefData(TypeRefData(_, "Bytes", "Com.Expload"), "Length", _)) =>
      Right(List(Operation(Opcodes.LENGTH)))
    case _ => Left(UnknownOpcode)
  }
}
