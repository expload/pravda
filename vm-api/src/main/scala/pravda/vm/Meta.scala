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
    case LabelDef(name)     => s"""labelDef ${Utf8(name).mkString()}"""
    case LabelUse(name)     => s"""labelUse ${Utf8(name).mkString()}"""
    case m: MethodSignature => s"method ${m.toStruct.mkString()}"
    case ProgramName(name)  => s"""programName ${Utf8(name).mkString()}"""
    case Custom(name)       => s"""custom ${Utf8(name).mkString()}"""
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

    val nameKey = Data.Primitive.Int8(0xff.toByte)
    val returnTpeKey = Data.Primitive.Int8(0xfe.toByte)

    val maxArgs = 0x7e // 0xfd / 2

    def fromStruct(struct: Data.Struct): MethodSignature = {
      assert(struct.data.contains(nameKey))
      assert(struct.data.contains(returnTpeKey))

      val name = struct.data.getOrElse(nameKey, throw Data.InvalidData(struct)) match {
        case Data.Primitive.Utf8(n) => n
        case _                      => throw Data.InvalidData(struct)
      }

      val returnTpe = struct.data.getOrElse(returnTpeKey, throw Data.InvalidData(struct)) match {
        case p: Data.Primitive => TypeSignature.fromPrimivite(p)
        case _                 => throw Data.InvalidData(struct)
      }

      val args = for {
        i <- 0 to maxArgs
        pi = Data.Primitive.Int8((2 * i).toByte)
        pNameI = Data.Primitive.Int8((2 * i + 1).toByte)
        if struct.data.contains(pi)
      } yield {
        struct.data(pi) match {
          case p: Data.Primitive =>
            val argName = struct.data.get(pNameI).flatMap {
              case Data.Primitive.Utf8(s) => Some(s)
              case _                      => None
            }
            val argType = TypeSignature.fromPrimivite(p)
            (argName, argType)
          case _ => throw Data.InvalidData(struct)
        }
      }

      MethodSignature(name, returnTpe, args.toList)
    }
  }

  final case class LabelDef(name: String) extends Meta
  final case class LabelUse(name: String) extends Meta
  final case class MethodSignature(name: String, returnTpe: TypeSignature, args: List[(Option[String], TypeSignature)])
      extends Meta {

    lazy val argNames: List[String] = args.zipWithIndex.map { case ((n, _), i) => n.getOrElse(s"arg$i") }
    lazy val argTpes: List[TypeSignature] = args.map(_._2)

    def toStruct: Data.Struct = {
      assert(args.length <= MethodSignature.maxArgs, "Too many args in method.")

      Data.Struct(
        mutable.Map[Data.Primitive, Data.Primitive](
          MethodSignature.nameKey -> Data.Primitive.Utf8(name),
          MethodSignature.returnTpeKey -> returnTpe.toPrimitive
        ) ++ args.zipWithIndex.flatMap {
          case ((argName, arg), i) =>
            (Data.Primitive.Int8((2 * i).toByte) -> arg.toPrimitive) ::
              argName
              .map(n => Data.Primitive.Int8((2 * i + 1).toByte) -> Data.Primitive.Utf8(n))
              .toList
        }
      )
    }
  }
  final case class ProgramName(name: String) extends Meta
  final case class Custom(name: String)      extends Meta

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
      case TypeLabelDef    => LabelDef(readString())
      case TypeLabelUse    => LabelUse(readString())
      case TypeCustom      => Custom(readString())
      case TypeMethod      => MethodSignature.fromStruct(readStruct())
      case TypeProgramName => ProgramName(readString())
    }
  }

  object parser {

    val meta: P[Meta] = P(
      ("labelDef " ~ Data.parser.utf8.map(s => LabelDef(s.data))) |
        ("labelUse " ~ Data.parser.utf8.map(s => LabelUse(s.data))) |
        ("programName " ~ Data.parser.utf8.map(s => ProgramName(s.data))) |
        ("custom " ~ Data.parser.utf8.map(s => Custom(s.data))) |
        ("method " ~ Data.parser.struct.map(MethodSignature.fromStruct))
    )
  }

  final val TypeLabelDef = 0x00
  final val TypeLabelUse = 0x01
  final val TypeMethod = 0x02
  final val TypeProgramName = 0x03
  final val TypeCustom = 0xFF
}
