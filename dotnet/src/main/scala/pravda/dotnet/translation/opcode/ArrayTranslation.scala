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
import pravda.dotnet.parser.CIL._
import pravda.dotnet.parser.PE
import pravda.dotnet.parser.Signatures.{FieldSig, SigType}
import pravda.dotnet.translation.data.{InnerTranslationError, MethodTranslationCtx, UnknownOpcode}
import pravda.vm.{Data, Opcodes}
import pravda.vm.asm.Operation
import scodec.bits.ByteOrdering

import scala.util.Try

object ArrayTranslation extends OneToManyTranslatorOnlyAsm {

  override def asmOpsOne(op: Op,
                         stackOffsetO: Option[Int],
                         ctx: MethodTranslationCtx): Either[InnerTranslationError, List[Operation]] = {
    op match {
      case NewArr(TypeRefData(6, typeName, namespaceName)) =>
        val arrTypeF: PartialFunction[(String, String), Operation] = {
          case ("System", "Byte")   => pushType(Data.Type.Int8)
          case ("System", "Char")   => pushType(Data.Type.Int16)
          case ("System", "Int32")  => pushType(Data.Type.Int32)
          case ("System", "UInt32") => pushType(Data.Type.Uint32)
          case ("System", "Double") => pushType(Data.Type.Number)
          case ("System", "String") => pushType(Data.Type.Utf8)
        }

        val asmOps = arrTypeF
          .lift((namespaceName, typeName))
          .map(List(_, Operation(Opcodes.NEW_ARRAY)))
          .toRight(UnknownOpcode)

        asmOps
      case StElem(_) | StElemI1 | StElemI2 | StElemI4 | StElemI8 | StElemR4 | StElemR8 | StElemRef =>
        Right(List(Operation(Opcodes.SWAP), Operation(Opcodes.ARRAY_MUT)))
      case LdElem(_) | LdElemI1 | LdElemI2 | LdElemI4 | LdElemI8 | LdElemR4 | LdElemR8 | LdElemRef | LdElemU1 |
          LdElemU2 | LdElemU4 | LdElemU8 =>
        Right(List(Operation(Opcodes.ARRAY_GET)))
      case LdLen =>
        Right(List(Operation(Opcodes.LENGTH)))
      case _ =>
        Left(UnknownOpcode)
    }
  }
}

object ArrayInitializationTranslation extends OpcodeTranslatorOnlyAsm {
  override def asmOps(ops: List[Op],
                      stackOffsetO: Option[Int],
                      ctx: MethodTranslationCtx): Either[InnerTranslationError, (Int, List[Operation])] = {
    ops.take(5) match {
      case List(
          OpcodeDetectors.IntLoad(arraySize),
          NewArr(TypeRefData(_, typeName, namespaceName)),
          Dup,
          LdToken(FieldData(_, _, fieldName, tokenSignIdx)),
          Call(MemberRefData(TypeRefData(_, "RuntimeHelpers", "System.Runtime.CompilerServices"), "InitializeArray", _))
          ) =>
        def bytesRva =
          for {
            rva <- ctx.tctx.cilData.tables.fieldRVATable.find(_.field.name == fieldName)
          } yield rva.rva

        def bytesSize =
          for {
            token <- ctx.tctx.signatures.get(tokenSignIdx)
            size <- token match {
              case FieldSig(SigType.ValueTpe(TypeDefData(_, _, fieldType, "", _, Vector(), Vector())))
                  if fieldType.startsWith("__StaticArrayInitTypeSize=") =>
                Try { fieldType.drop("__StaticArrayInitTypeSize=".length).toLong }.toOption
              case FieldSig(SigType.I1) => Some(1L)
              case FieldSig(SigType.I2) => Some(2L)
              case FieldSig(SigType.I4) => Some(4L)
              case FieldSig(SigType.U8) => Some(8L)
              case _                    => None
            }
          } yield size

        def data(bytes: fastparse.byte.all.Bytes): Option[Data] = (namespaceName, typeName) match {
          case ("System", "Byte") => Some(Data.Array.Int8Array(bytes.toArray.toBuffer))
          case ("System", "Char") =>
            Some(Data.Array.Int16Array(bytes.grouped(2).map(_.toShort(ordering = ByteOrdering.LittleEndian)).toBuffer))
          case ("System", "Int32") =>
            Some(Data.Array.Int32Array(bytes.grouped(4).map(_.toInt(ordering = ByteOrdering.LittleEndian)).toBuffer))
          case ("System", "UInt32") =>
            Some(
              Data.Array.Int32Array(
                bytes.grouped(4).map(_.toInt(signed = false, ordering = ByteOrdering.LittleEndian)).toBuffer))
          case ("System", "Double") =>
            Some(Data.Array.NumberArray(bytes.grouped(8).map(_.reverse.toByteBuffer.getDouble).toBuffer))
          case _ => None
        }

        (for {
          rva <- bytesRva
          size <- bytesSize
          bytes = PE.bytesFromRva(ctx.tctx.cilData.sections, rva, Some(size))
          d <- data(bytes)
        } yield (5, List(Operation.New(d)))).toRight(UnknownOpcode)
      case _ => Left(UnknownOpcode)
    }
  }
}
