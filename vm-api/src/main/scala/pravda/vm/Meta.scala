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

package pravda.vm

import java.nio.ByteBuffer

import Data.Primitive._
import com.google.protobuf.ByteString

import scala.annotation.switch
import scala.collection.mutable
import fastparse.all._

sealed trait Meta {

  import Meta._

  def mkString: String = this match {
    case LabelDef(name)       => s"label_def ${Utf8(name).mkString()}"
    case LabelUse(name)       => s"label_use ${Utf8(name).mkString()}"
    case m: MethodSignature   => s"method ${m.toStruct.mkString()}"
    case ProgramName(name)    => s"program_name ${Utf8(name).mkString()}"
    case s: SourceMark        => s"source_mark ${s.toStruct.mkString()}"
    case TranslatorMark(mark) => s"translator_mark ${Utf8(mark).mkString()}"
    case Custom(name)         => s"custom ${Utf8(name).mkString()}"
  }

  def writeToByteBuffer(buffer: ByteBuffer): Unit = this match {
    case LabelDef(name) =>
      buffer.put(TypeLabelDef.toByte)
      Data.Primitive.Utf8(name).writeToByteBuffer(buffer)
    case LabelUse(name) =>
      buffer.put(TypeLabelUse.toByte)
      Utf8(name).writeToByteBuffer(buffer)
    case m: MethodSignature =>
      buffer.put(TypeMethod.toByte)
      m.toStruct.writeToByteBuffer(buffer)
    case ProgramName(name) =>
      buffer.put(TypeProgramName.toByte)
      Utf8(name).writeToByteBuffer(buffer)
    case s: SourceMark =>
      buffer.put(TypeSourceMark.toByte)
      s.toStruct.writeToByteBuffer(buffer)
    case TranslatorMark(mark) =>
      buffer.put(TypeTranslatorMark.toByte)
      Utf8(mark).writeToByteBuffer(buffer)
    case Custom(name) =>
      buffer.put(TypeCustom.toByte)
      Utf8(name).writeToByteBuffer(buffer)
  }
}

object Meta {

  sealed trait TypeSignature {
    import TypeSignature._

    def toPrimitive: Data.Primitive = this match {
      case p: PrimitiveType =>
        Data.Primitive.Int8(p.typeByte)
      case Array(p) =>
        val arr = new scala.Array[Byte](2)
        arr(0) = Data.Type.Array
        arr(1) = p.typeByte
        Data.Primitive.Bytes(ByteString.copyFrom(arr))
      case _ => throw Data.UnknownTypeException(Data.Type.Struct, 0)
    }

    def mnemonic: String = this match {
      case Null     => "null"
      case Int8     => "int8"
      case Int16    => "int16"
      case Int32    => "int32"
      case BigInt   => "bigint"
      case Uint8    => "uint8"
      case Uint16   => "uint16"
      case Uint32   => "uint32"
      case Number   => "number"
      case Boolean  => "boolean"
      case Ref      => "ref"
      case Utf8     => "utf8"
      case Bytes    => "bytes"
      case Array(p) => s"array ${p.mnemonic}"
      case _        => throw Data.UnknownTypeException(Data.Type.Struct, 0)
    }
  }

  object TypeSignature {
    sealed abstract class PrimitiveType(val typeByte: Byte)                      extends TypeSignature
    case object Null                                                             extends PrimitiveType(Data.Type.Null)
    case object Int8                                                             extends PrimitiveType(Data.Type.Int8)
    case object Int16                                                            extends PrimitiveType(Data.Type.Int16)
    case object Int32                                                            extends PrimitiveType(Data.Type.Int32)
    case object BigInt                                                           extends PrimitiveType(Data.Type.BigInt)
    case object Uint8                                                            extends PrimitiveType(Data.Type.Uint8)
    case object Uint16                                                           extends PrimitiveType(Data.Type.Uint16)
    case object Uint32                                                           extends PrimitiveType(Data.Type.Uint32)
    case object Number                                                           extends PrimitiveType(Data.Type.Number)
    case object Boolean                                                          extends PrimitiveType(Data.Type.Boolean)
    case object Ref                                                              extends PrimitiveType(Data.Type.Ref)
    case object Utf8                                                             extends PrimitiveType(Data.Type.Utf8)
    case object Bytes                                                            extends PrimitiveType(Data.Type.Bytes)
    final case class Array(primitive: PrimitiveType)                             extends TypeSignature
    final case class Struct(name: String, fields: List[(String, PrimitiveType)]) extends TypeSignature

    def fromPrimivite(primitive: Data.Primitive): TypeSignature = {
      def primitiveFromByte(byte: Byte): Option[PrimitiveType] = byte match {
        case Data.Type.Null    => Some(Null)
        case Data.Type.Int8    => Some(Int8)
        case Data.Type.Int16   => Some(Int16)
        case Data.Type.Int32   => Some(Int32)
        case Data.Type.BigInt  => Some(BigInt)
        case Data.Type.Uint8   => Some(Uint8)
        case Data.Type.Uint16  => Some(Uint16)
        case Data.Type.Uint32  => Some(Uint32)
        case Data.Type.Number  => Some(Number)
        case Data.Type.Boolean => Some(Boolean)
        case Data.Type.Ref     => Some(Ref)
        case Data.Type.Utf8    => Some(Utf8)
        case Data.Type.Bytes   => Some(Bytes)
        case _                 => None
      }

      primitive match {
        case Data.Primitive.Int8(b) => primitiveFromByte(b).getOrElse(throw Data.InvalidData(primitive))
        case Data.Primitive.Bytes(bytes) =>
          bytes.byteAt(0) match {
            case Data.Type.Array =>
              primitiveFromByte(bytes.byteAt(1)).map(Array).getOrElse(throw Data.InvalidData(primitive))
            case other => throw Data.InvalidData(primitive)
          }
        case _ => throw Data.InvalidData(primitive)
      }
    }
  }

  object MethodSignature {

    final val NameKey = Data.Primitive.Utf8("name")
    final val ReturnTpeKey = Data.Primitive.Utf8("returnTpe")

    def fromStruct(struct: Data.Struct): MethodSignature = {
      assert(struct.data.contains(NameKey))
      assert(struct.data.contains(ReturnTpeKey))

      val name = struct.data.getOrElse(NameKey, throw Data.InvalidData(struct)) match {
        case Data.Primitive.Utf8(n) => n
        case _                      => throw Data.InvalidData(struct)
      }

      val returnTpe = struct.data.getOrElse(ReturnTpeKey, throw Data.InvalidData(struct)) match {
        case p: Data.Primitive => TypeSignature.fromPrimivite(p)
        case _                 => throw Data.InvalidData(struct)
      }

      val args = struct.data.toList
        .collect {
          case (Data.Primitive.Int32(i), tpe) => (i, tpe)
        }
        .sortBy(_._1)
        .map { case (_, tpe) => TypeSignature.fromPrimivite(tpe) }

      MethodSignature(name, returnTpe, args)
    }
  }

  object SourceMark {

    def fromStruct(struct: Data.Struct): SourceMark = {
      def getIntOrThrow(field: String): Int =
        struct.data
          .get(Data.Primitive.Utf8(field))
          .collect { case Data.Primitive.Int32(i) => i }
          .getOrElse(throw Data.InvalidData(struct))

      SourceMark(
        struct.data
          .get(Data.Primitive.Utf8("src"))
          .collect { case Data.Primitive.Utf8(s) => s }
          .getOrElse(throw Data.InvalidData(struct)),
        getIntOrThrow("sl"),
        getIntOrThrow("sc"),
        getIntOrThrow("el"),
        getIntOrThrow("ec")
      )
    }
  }

  final case class LabelDef(name: String) extends Meta
  final case class LabelUse(name: String) extends Meta
  final case class MethodSignature(name: String, returnTpe: TypeSignature, args: List[TypeSignature]) extends Meta {

    def toStruct: Data.Struct = {
      Data.Struct(
        mutable.Map[Data.Primitive, Data.Primitive](
          MethodSignature.NameKey -> Data.Primitive.Utf8(name),
          MethodSignature.ReturnTpeKey -> returnTpe.toPrimitive
        ) ++ args.zipWithIndex.map { case (arg, i) => Data.Primitive.Int32(i) -> arg.toPrimitive }
      )
    }
  }
  final case class ProgramName(name: String) extends Meta
  final case class SourceMark(source: String, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int)
      extends Meta {

    def toStruct: Data.Struct =
      Data.Struct(
        mutable.Map(
          Data.Primitive.Utf8("src") -> Data.Primitive.Utf8(source),
          Data.Primitive.Utf8("sl") -> Data.Primitive.Int32(startLine),
          Data.Primitive.Utf8("sc") -> Data.Primitive.Int32(startColumn),
          Data.Primitive.Utf8("el") -> Data.Primitive.Int32(endLine),
          Data.Primitive.Utf8("ec") -> Data.Primitive.Int32(endColumn)
        ))
  }
  final case class TranslatorMark(mark: String) extends Meta
  final case class Custom(name: String)         extends Meta

  def readFromByteBuffer(buffer: ByteBuffer): Meta = {

    def readData[T](toData: PartialFunction[Data, T]) = {
      val value = Data.readFromByteBuffer(buffer)
      toData.applyOrElse(value, (_: Data) => throw Data.UnexpectedTypeException(value.getClass, buffer.position))
    }

    def readString() = readData[String] {
      case Data.Primitive.Utf8(s) => s
    }

    def readStruct() = readData[Data.Struct] {
      case s: Data.Struct => s
    }

    (buffer.get & 0xFF: @switch) match {
      case TypeLabelDef       => LabelDef(readString())
      case TypeLabelUse       => LabelUse(readString())
      case TypeCustom         => Custom(readString())
      case TypeMethod         => MethodSignature.fromStruct(readStruct())
      case TypeSourceMark     => SourceMark.fromStruct(readStruct())
      case TypeTranslatorMark => TranslatorMark(readString())
      case TypeProgramName    => ProgramName(readString())
    }
  }

  def externalReadFromByteBuffer(buffer: ByteBuffer): Map[Long, List[Meta]] = {
    val res = mutable.Map.empty[Long, List[Meta]]
    while (buffer.hasRemaining) {
      val offset = buffer.getLong
      val meta = Meta.readFromByteBuffer(buffer)
      if (res.contains(offset)) {
        res.put(offset, meta :: res(offset))
      } else {
        res.put(offset, List(meta))
      }
    }

    res.toMap
  }

  def externalWriteToByteBuffer(buffer: ByteBuffer, metas: List[(Long, Meta)]): Unit = {
    metas.foreach {
      case (offset, meta) =>
        buffer.putLong(offset)
        meta.writeToByteBuffer(buffer)
    }
  }

  object parser {

    val meta: P[Meta] = P(
      ("label_def " ~ Data.parser.utf8.map(s => LabelDef(s.data))) |
        ("label_use " ~ Data.parser.utf8.map(s => LabelUse(s.data))) |
        ("program_name " ~ Data.parser.utf8.map(s => ProgramName(s.data))) |
        ("custom " ~ Data.parser.utf8.map(s => Custom(s.data))) |
        ("method " ~ Data.parser.struct.map(MethodSignature.fromStruct)) |
        ("source_mark " ~ Data.parser.struct.map(SourceMark.fromStruct)) |
        ("translator_mark " ~ Data.parser.utf8.map(s => TranslatorMark(s.data)))
    )
  }

  final val TypeLabelDef = 0x00
  final val TypeLabelUse = 0x01
  final val TypeMethod = 0x02
  final val TypeProgramName = 0x03
  final val TypeSourceMark = 0x04
  final val TypeTranslatorMark = 0x05
  final val TypeCustom = 0xFF
}
