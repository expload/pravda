package pravda.vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.bytes.{byteString2hex, hex2byteString}
import supertagged.TaggedType
import scala.annotation.{strictfp, switch, tailrec}
import scala.collection.mutable
import scala.{Array => ScalaArray, BigInt => ScalaBigInt}

@strictfp sealed trait Data {

  import Data._
  import Array._
  import Primitive._

  // TODO optimize me. better way to save count of bytes on deserialization
  lazy val volume: Int = {
    val buffer = getByteStringBuffer
    writeToByteBuffer(buffer)
    buffer.flip()
    buffer.remaining()
  }

  def mkString(untypedNumerics: Boolean = false, escapeUnicode: Boolean = false, pretty: Boolean = false): String = {

    val comma = if (pretty) ", " else ","

    def escape(s: String) = s flatMap {
      case '"'  => "\\\""
      case '\\' => "\\\\"
      case '\b' => "\\b"
      case '\f' => "\\f"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c =>
        if (c < ' ' || (escapeUnicode && c > '~')) "\\u%04x" format c.toInt
        else c.toString
    }

    def ref(x: Int): String = s"0x${(x & 0xFFFFFFFFl).toHexString.toUpperCase}"
    def string(x: String): String = s""""${escape(x)}""""
    def bytes(x: ByteString): String = byteString2hex(x).toUpperCase
    def array[T](xs: Seq[T]): String = xs.mkString(comma)
    def arraym[T](xs: Seq[T], f: T => String): String = xs.map(f).mkString(comma)
    def bool(x: Bool): String = x match {
      case Bool.True  => "true"
      case Bool.False => "false"
    }

    this match {
      // Primitives
      case numeric: Numeric[_] if untypedNumerics =>
        numeric.data.toString
      case Null         => "null"
      case Int8(data)   => s"int8($data)"
      case Int16(data)  => s"int16($data)"
      case Int32(data)  => s"int32($data)"
      case Uint8(data)  => s"uint8($data)"
      case Uint16(data) => s"uint16($data)"
      case Uint32(data) => s"uint32($data)"
      case Number(data) => s"number($data)"
      case BigInt(data) => s"bigint($data)"
      case Ref(data)    => s"#${ref(data)}"
      case Bytes(data)  => s"x${bytes(data)}"
      case Bool.True    => "true"
      case Bool.False   => "false"
      // Arrays
      case Int8Array(data)              => s"int8[${array(data)}]"
      case Int16Array(data)             => s"int16[${array(data)}]"
      case Int32Array(data)             => s"int32[${array(data)}]"
      case Uint8Array(data)             => s"uint8[${array(data)}]"
      case Uint16Array(data)            => s"uint16[${array(data)}]"
      case Uint32Array(data)            => s"uint32[${array(data)}]"
      case NumberArray(data)            => s"number[${array(data)}]"
      case BigIntArray(data)            => s"bigint[${array(data)}]"
      case RefArray(data)               => s"#[${arraym(data, ref)}]"
      case BoolArray(data)              => s"bool[${arraym(data, bool)}]"
      case Utf8Array(data)              => s"utf8[${arraym(data, string)}]"
      case BytesArray(data)             => s"x[${arraym(data, bytes)}]"
      case Utf8(data)                   => string(data)
      case Struct(data) if data.isEmpty => "{}"
      case Struct(data) =>
        val commaNl = if (pretty) ",\n" else ","
        val xs = data map {
          case (k, v) =>
            val pk = k.mkString(untypedNumerics, escapeUnicode, pretty)
            val pv = v.mkString(untypedNumerics, escapeUnicode, pretty)
            if (pretty) s"  $pk: $pv"
            else s"$pk:$pv"
        }
        s"{\n${xs.mkString(commaNl)}\n}"
    }
  }

  def toByteString: ByteString = {
    val buffer = getByteStringBuffer
    writeToByteBuffer(buffer)
    buffer.flip()
    ByteString.copyFrom(buffer)
  }

  def writeToByteBuffer(buffer: ByteBuffer): Unit = {

    def isZeroLength(value: Long) = value > 0 && value < 64

    /** @param value byte < 64 */
    def putZeroLengthValue(value: Byte) = {
      //println(s"putZeroLengthValue: value = $value")
      if (!isZeroLength(value.toLong))
        throw new IllegalArgumentException(s"`value` should be less than 64 but it is $value")
      buffer.put(value)
    }

    def putLength(buffer: ByteBuffer, length: Int): Unit = {
      //println(s"putLength: $length")
      if (length < 0 || length >= 0x400000)
        throw new IllegalArgumentException(s"`length` should be greater than 0 and less than 4194304 but it is $length")
      val pb = getLengthBuffer
      pb.rewind()
      pb.putInt(length)
      pb.flip()
      seek(pb)
      //println(s"putLength: pb.remaining = ${pb.remaining}")
      (pb.remaining: @switch) match {
        case 0 =>
          buffer.put(0.toByte)
        case 1 =>
          //println(s"putLength: pb.get = ${pb.get(pb.position)}")
          if ((pb.get(pb.position) & 0xFF) < 64) {
            buffer.put((pb.get | 0x40).toByte)
          } else {
            //println(s"putLength: x = ${pb.get(pb.position()) & 0xFF}")
            buffer.put(0x80.toByte)
            buffer.put(pb.get)
          }
        case 2 =>
          // 2 bytes of length including header
          // 14 bits for length and 2 bits for length header
          if (length < 0x4000) {
            val x = pb.getShort & 0xFFFF
            buffer.putShort((0x8000 | x).toShort)
          } else {
            buffer.put(0xC0.toByte)
            buffer.putShort(pb.getShort)
          }
        case 3 =>
          val fst = pb.get & 0xFF
          val snd = pb.get & 0xFF
          val thd = pb.get & 0xFF
          buffer.put((0xC0 | fst).toByte)
          buffer.put(snd.toByte)
          buffer.put(thd.toByte)
      }
    }

    def putBytes(buffer: ByteBuffer, data: ByteString) = {
      putLength(buffer, data.size())
      buffer.put(data.asReadOnlyByteBuffer())
    }

    def putString(buffer: ByteBuffer, data: String) = {
      val bytes = data.getBytes
      putLength(buffer, bytes.length)
      buffer.put(bytes)
    }

    def putRef(x: Int): Unit = {
      buffer.put(Type.Ref)
      buffer.putInt(x)
    }

    def putPrimitive(typeTag: Type)(f: ByteBuffer => Unit): Unit = {
      buffer.put(typeTag)
      putTaglessPrimitive(f)
    }

    def putTaglessPrimitive[U](f: ByteBuffer => U): Unit = {
      val pb = getPrimitiveBuffer
      f(pb)
      pb.flip()
      seek(pb)
      val l = pb.remaining
      //println(s"putTaglessPrimitive: pb.remaining = ${pb.remaining}")
      if (l == 0) {
        buffer.put(0.toByte)
      } else if (l == 1 && isZeroLength(pb.get(pb.position).toLong)) {
        putZeroLengthValue(pb.get(pb.position))
      } else {
        putLength(buffer, l)
        buffer.put(pb)
      }
    }

    def putPrimitiveArray[T, U](typeTag: Type, xs: Seq[T])(f: (ByteBuffer, T) => U): Unit = {
      buffer.put(Type.Array)
      buffer.put(typeTag)
      putLength(buffer, xs.length)
      xs.foreach(x => putTaglessPrimitive(f(_, x)))
    }

    def putArray[T, U](typeTag: Type, xs: Seq[T])(f: (ByteBuffer, T) => U): Unit = {
      buffer.put(Type.Array)
      buffer.put(typeTag)
      putLength(buffer, xs.length)
      xs.foreach(x => f(buffer, x))
    }

    def putTaggedBigInt(buffer: ByteBuffer, value: ScalaBigInt): Unit = {
      buffer.put(Type.BigInt)
      putBigInt(buffer, value)
    }

    def putBigInt(buffer: ByteBuffer, value: ScalaBigInt): Unit = {
      val bytes = value.toByteArray
      putLength(buffer, bytes.length)
      buffer.put(bytes)
    }

    def putBoolean(buffer: ByteBuffer, value: Byte) = {
      buffer.put(Type.Boolean)
      buffer.put(value)
    }

    this match {
      case Null              => buffer.put(Type.Null)
      case Int8(data)        => putPrimitive(Type.Int8)(_.put(data))
      case Int16(data)       => putPrimitive(Type.Int16)(_.putShort(data))
      case Int32(data)       => putPrimitive(Type.Int32)(_.putInt(data))
      case Uint8(data)       => putPrimitive(Type.Uint8)(_.putInt(data))
      case Uint16(data)      => putPrimitive(Type.Uint16)(_.putInt(data))
      case Uint32(data)      => putPrimitive(Type.Uint32)(_.putLong(data))
      case BigInt(data)      => putTaggedBigInt(buffer, data)
      case Ref(data)         => putRef(data)
      case Bool.True         => putBoolean(buffer, 1.toByte)
      case Bool.False        => putBoolean(buffer, 0.toByte)
      case Int8Array(data)   => putPrimitiveArray(Type.Int8, data)(_.put(_))
      case Int16Array(data)  => putPrimitiveArray(Type.Int16, data)(_.putShort(_))
      case Int32Array(data)  => putPrimitiveArray(Type.Int32, data)(_.putInt(_))
      case Uint8Array(data)  => putPrimitiveArray(Type.Uint8, data)(_.putInt(_))
      case Uint16Array(data) => putPrimitiveArray(Type.Uint16, data)(_.putInt(_))
      case Uint32Array(data) => putPrimitiveArray(Type.Uint32, data)(_.putLong(_))
      case BigIntArray(data) => putArray(Type.BigInt, data)(putBigInt)
      case RefArray(data)    => putArray(Type.Ref, data)(_.putInt(_))
      case Number(data)      => putPrimitive(Type.Number)(_.putDouble(data))
      case NumberArray(data) => putPrimitiveArray(Type.Number, data)(_.putDouble(_))
      case Utf8Array(data)   => putArray(Type.Utf8, data)(putString)
      case BytesArray(data)  => putArray(Type.Bytes, data)(putBytes)
      case BoolArray(data) =>
        putPrimitiveArray(Type.Boolean, data) { (b, x) =>
          if (x == Bool.True) b.put(1.toByte)
          else b.put(0.toByte)
        }
      case Bytes(data) =>
        buffer.put(Type.Bytes)
        putBytes(buffer, data)
      case Utf8(data) =>
        buffer.put(Type.Utf8)
        putString(buffer, data)
      case Struct(data) =>
        buffer.put(Type.Struct)
        putLength(buffer, data.size)
        data.foreach {
          case (key, value) =>
            key.writeToByteBuffer(buffer)
            value.writeToByteBuffer(buffer)
        }
      case MarshalledData(data) =>
        buffer.put(data.toByteArray)
    }
  }

}

@strictfp object Data {

  sealed trait Primitive extends Data {

    import Primitive._

    def cast(`type`: Type): Data.Primitive = this match {
      case value @ Int8(data) =>
        `type` match {
          case Type.Int8    => value
          case Type.Int16   => Int16(data.toShort)
          case Type.Int32   => Int32(data.toInt)
          case Type.Uint8   => Uint8(data & 0xFF)
          case Type.Uint16  => Uint16(data & 0xFF)
          case Type.Uint32  => Uint32(data & 0xFFl)
          case Type.BigInt  => BigInt(scala.BigInt(data.toInt))
          case Type.Number  => Number(data.toDouble)
          case Type.Ref     => Ref(data.toInt)
          case Type.Boolean => if (data == 0) Bool.False else Bool.True
          case Type.Utf8    => Utf8(data.toString)
          case Type.Bytes =>
            val array = new scala.Array[Byte](1)
            array(0) = data
            Bytes(ByteString.copyFrom(array))
        }
      case value @ Int16(data) =>
        `type` match {
          case Type.Int8    => Int8(data.toByte)
          case Type.Int16   => value
          case Type.Int32   => Int32(data.toInt)
          case Type.Uint8   => Uint8(data & 0xFF)
          case Type.Uint16  => Uint16(data & 0xFFFF)
          case Type.Uint32  => Uint32(data & 0xFFFFl)
          case Type.BigInt  => BigInt(scala.BigInt(data.toInt))
          case Type.Number  => Number(data.toDouble)
          case Type.Ref     => Ref(data.toInt)
          case Type.Boolean => if (data == 0) Bool.False else Bool.True
          case Type.Utf8    => Utf8(data.toString)
          case Type.Bytes =>
            val buffer = ByteBuffer.allocate(2)
            buffer.putShort(data)
            buffer.rewind()
            Bytes(ByteString.copyFrom(buffer))
        }
      case value @ Int32(data) =>
        `type` match {
          case Type.Int8    => Int8(data.toByte)
          case Type.Int16   => Int16(data.toShort)
          case Type.Int32   => value
          case Type.Uint8   => Uint8(data & 0xFFFFFFFF)
          case Type.Uint16  => Uint16(data & 0xFFFFFFFF)
          case Type.Uint32  => Uint32(data.toLong & 0xFFFFFFFFl)
          case Type.BigInt  => BigInt(scala.BigInt(data))
          case Type.Number  => Number(data.toDouble)
          case Type.Ref     => Ref(data)
          case Type.Boolean => if (data == 0) Bool.False else Bool.True
          case Type.Utf8    => Utf8(data.toString)
          case Type.Bytes =>
            val buffer = ByteBuffer.allocate(4)
            buffer.putInt(data)
            buffer.rewind()
            Bytes(ByteString.copyFrom(buffer))
        }
      case value @ Uint8(data) =>
        `type` match {
          case Type.Int8    => Int8(data.toByte)
          case Type.Int16   => Int16(data.toShort)
          case Type.Int32   => Int32(data.toInt)
          case Type.Uint8   => value
          case Type.Uint16  => Uint16(data)
          case Type.Uint32  => Uint32(data.toLong)
          case Type.BigInt  => BigInt(scala.BigInt(data))
          case Type.Number  => Number(data.toDouble)
          case Type.Ref     => Ref(data.toInt)
          case Type.Boolean => if (data == 0) Bool.False else Bool.True
          case Type.Utf8    => Utf8(data.toString)
          case Type.Bytes =>
            val array = new scala.Array[Byte](1)
            array(0) = data.toByte
            Bytes(ByteString.copyFrom(array))
        }
      case value @ Uint16(data) =>
        `type` match {
          case Type.Int8    => Int8(data.toByte)
          case Type.Int16   => Int16(data.toShort)
          case Type.Int32   => Int32(data.toInt)
          case Type.Uint8   => Uint8(data & 0xFF)
          case Type.Uint16  => value
          case Type.Uint32  => Uint32(data.toLong)
          case Type.BigInt  => BigInt(scala.BigInt(data))
          case Type.Number  => Number(data.toDouble)
          case Type.Ref     => Ref(data.toInt)
          case Type.Boolean => if (data == 0) Bool.False else Bool.True
          case Type.Utf8    => Utf8(data.toString)
          case Type.Bytes =>
            val buffer = ByteBuffer.allocate(2)
            buffer.putShort(data.toShort)
            buffer.rewind()
            Bytes(ByteString.copyFrom(buffer))
        }
      case value @ Uint32(data) =>
        `type` match {
          case Type.Int8    => Int8(data.toByte)
          case Type.Int16   => Int16(data.toShort)
          case Type.Int32   => Int32(data.toInt)
          case Type.Uint8   => Uint8((data & 0xFF).toInt)
          case Type.Uint16  => Uint16((data & 0xFFFF).toInt)
          case Type.Uint32  => value
          case Type.BigInt  => BigInt(scala.BigInt(data))
          case Type.Number  => Number(data.toDouble)
          case Type.Ref     => Ref(data.toInt)
          case Type.Boolean => if (data == 0) Bool.False else Bool.True
          case Type.Utf8    => Utf8(data.toString)
          case Type.Bytes =>
            val buffer = ByteBuffer.allocate(4)
            buffer.putInt(data.toInt)
            buffer.rewind()
            Bytes(ByteString.copyFrom(buffer))
        }
      case value @ Ref(data) =>
        `type` match {
          case Type.Int8    => Int8(data.toByte)
          case Type.Int16   => Int16(data.toShort)
          case Type.Int32   => Int32(data)
          case Type.Uint8   => Uint8(data & 0xFF)
          case Type.Uint16  => Uint16(data & 0xFFFF)
          case Type.Uint32  => Uint32(data.toLong & 0xFFFFFFFFl)
          case Type.BigInt  => BigInt(scala.BigInt(data))
          case Type.Number  => Number(data.toDouble)
          case Type.Ref     => value
          case Type.Boolean => if (data == 0) Bool.False else Bool.True
          case Type.Utf8    => Utf8(data.toString)
          case Type.Bytes =>
            val buffer = ByteBuffer.allocate(4)
            buffer.putInt(data)
            buffer.rewind()
            Bytes(ByteString.copyFrom(buffer))
        }
      case value @ Number(data) =>
        `type` match {
          case Type.Int8    => Int8(data.toByte)
          case Type.Int16   => Int16(data.toShort)
          case Type.Int32   => Int32(data.toInt)
          case Type.Uint8   => Uint8(data.toInt & 0xFF)
          case Type.Uint16  => Uint16(data.toInt & 0xFFFF)
          case Type.Uint32  => Uint32(data.toLong & 0xFFFFFFFFl)
          case Type.BigInt  => BigInt(scala.BigInt(data.toLong))
          case Type.Number  => value
          case Type.Ref     => Ref(data.toInt)
          case Type.Boolean => if (data == 0) Bool.False else Bool.True
          case Type.Utf8    => Utf8(data.toString)
          case Type.Bytes =>
            val buffer = ByteBuffer.allocate(8)
            buffer.putDouble(data)
            buffer.rewind()
            Bytes(ByteString.copyFrom(buffer))
        }
      case value @ Utf8(data) =>
        `type` match {
          case Type.Int8    => Int8(data.toByte)
          case Type.Int16   => Int16(data.toShort)
          case Type.Int32   => Int32(data.toInt)
          case Type.Uint8   => Uint8(data.toInt & 0xFF)
          case Type.Uint16  => Uint16(data.toInt & 0xFFFF)
          case Type.Uint32  => Uint32(data.toLong & 0xFFFFFFFFl)
          case Type.BigInt  => BigInt(scala.BigInt(data.toLong))
          case Type.Number  => Number(data.toDouble)
          case Type.Ref     => Ref(data.toInt)
          case Type.Boolean => if (data.isEmpty) Bool.False else Bool.True
          case Type.Utf8    => value
          case Type.Bytes   => Bytes(ByteString.copyFrom(data.getBytes))
        }
      case value @ Bytes(data) =>
        lazy val buffer = data.asReadOnlyByteBuffer()
        `type` match {
          case Type.Int8    => Int8(buffer.get)
          case Type.Int16   => Int16(buffer.getShort)
          case Type.Int32   => Int32(buffer.getInt)
          case Type.Uint8   => Uint8(buffer.get & 0xFF)
          case Type.Uint16  => Uint16(buffer.getShort & 0xFFFF)
          case Type.Uint32  => Uint32(buffer.getInt.toLong & 0xFFFFFFFFl)
          case Type.BigInt  => BigInt(scala.BigInt(data.toByteArray))
          case Type.Number  => Number(buffer.getDouble)
          case Type.Ref     => Ref(buffer.getInt)
          case Type.Boolean => if (data.isEmpty) Bool.False else Bool.True
          case Type.Utf8    => Utf8(new String(data.toByteArray))
          case Type.Bytes   => value
        }
      case value @ BigInt(data) =>
        `type` match {
          case Type.Int8    => Int8(data.toByte)
          case Type.Int16   => Int16(data.toShort)
          case Type.Int32   => Int32(data.toInt)
          case Type.Uint8   => Uint8(data.toInt & 0xFF)
          case Type.Uint16  => Uint16(data.toInt & 0xFFFF)
          case Type.Uint32  => Uint32(data.toLong & 0xFFFFFFFFl)
          case Type.BigInt  => value
          case Type.Number  => Number(data.toDouble)
          case Type.Ref     => Ref(data.toInt)
          case Type.Boolean => if (data == 0) Bool.False else Bool.True
          case Type.Utf8    => Utf8(data.toString)
          case Type.Bytes   => Bytes(ByteString.copyFrom(data.toByteArray))
        }
      case Bool.False =>
        `type` match {
          case Type.Int8    => Int8(0.toByte)
          case Type.Int16   => Int16(0.toShort)
          case Type.Int32   => Int32(0)
          case Type.Uint8   => Uint8(0)
          case Type.Uint16  => Uint16(0)
          case Type.Uint32  => Uint32(0L)
          case Type.BigInt  => BigInt(scala.BigInt(0))
          case Type.Number  => Number(0d)
          case Type.Boolean => Bool.False
          case Type.Utf8    => Utf8("false")
          case Type.Bytes =>
            val array = new scala.Array[Byte](1)
            array(0) = 0.toByte
            Bytes(ByteString.copyFrom(array))
        }
      case Bool.True =>
        `type` match {
          case Type.Int8    => Int8(1.toByte)
          case Type.Int16   => Int16(1.toShort)
          case Type.Int32   => Int32(1)
          case Type.Uint8   => Uint8(1)
          case Type.Uint16  => Uint16(1)
          case Type.Uint32  => Uint32(1L)
          case Type.BigInt  => BigInt(scala.BigInt(1))
          case Type.Number  => Number(1d)
          case Type.Boolean => Bool.True
          case Type.Utf8    => Utf8("true")
          case Type.Bytes =>
            val array = new scala.Array[Byte](1)
            array(0) = 1.toByte
            Bytes(ByteString.copyFrom(array))
        }
      case _ =>
        throw UnexpectedTypeException(this.getClass, -1)
    }
  }

  sealed trait Array extends Data
  sealed abstract class Numeric[T: scala.Numeric] extends Primitive {
    def data: T
  }

  object Primitive {

    final case class Int8(data: Byte)           extends Numeric[Byte]
    final case class Int16(data: Short)         extends Numeric[Short]
    final case class Int32(data: Int)           extends Numeric[Int]
    final case class Uint8(data: Int)           extends Numeric[Int]
    final case class Uint16(data: Int)          extends Numeric[Int]
    final case class Uint32(data: Long)         extends Numeric[Long]
    final case class BigInt(data: scala.BigInt) extends Numeric[scala.BigInt]
    final case class Number(data: Double)       extends Numeric[Double]
    final case class Utf8(data: String)         extends Primitive with Array
    final case class Bytes(data: ByteString)    extends Primitive with Array
    final case class Ref(data: Int)             extends Primitive

    case object Null  extends Primitive
    sealed trait Bool extends Primitive

    object Ref {
      final val Void = Ref(-1)
    }

    object Bool {

      def apply(value: Boolean): Bool =
        if (value) True else False

      final case object True  extends Bool
      final case object False extends Bool
    }
  }

  object Array {
    final case class Int8Array(data: mutable.Buffer[Byte])           extends Array
    final case class Int16Array(data: mutable.Buffer[Short])         extends Array
    final case class Int32Array(data: mutable.Buffer[Int])           extends Array
    final case class Uint8Array(data: mutable.Buffer[Int])           extends Array
    final case class Uint16Array(data: mutable.Buffer[Int])          extends Array
    final case class Uint32Array(data: mutable.Buffer[Long])         extends Array
    final case class BigIntArray(data: mutable.Buffer[scala.BigInt]) extends Array
    final case class NumberArray(data: mutable.Buffer[Double])       extends Array
    final case class RefArray(data: mutable.Buffer[Int])             extends Array
    final case class BoolArray(data: mutable.Buffer[Primitive.Bool]) extends Array
    final case class Utf8Array(data: mutable.Buffer[String])         extends Array
    final case class BytesArray(data: mutable.Buffer[ByteString])    extends Array
  }

  final case class Struct(data: mutable.Map[Primitive, Primitive]) extends Data
  final case class MarshalledData(data: ByteString)                extends Data

  // scalafix:off DisableSyntax.keywords.null

  //--------------------------------
  // Two 1M buffers for any purposes
  //--------------------------------

  private[vm] val primitiveBuffer = new ThreadLocal[ByteBuffer]()

  private[vm] def getPrimitiveBuffer = {
    val pb = primitiveBuffer.get()
    if (pb == null) {
      val newPb = ByteBuffer.allocate(256)
      primitiveBuffer.set(newPb)
      newPb
    } else {
      pb.clear()
      pb
    }
  }

  private[vm] val lengthBuffer = new ThreadLocal[ByteBuffer]()

  private[vm] def getLengthBuffer = {
    val pb = lengthBuffer.get()
    if (pb == null) {
      val newPb = ByteBuffer.allocate(4)
      lengthBuffer.set(newPb)
      newPb
    } else {
      pb.clear()
      pb
    }
  }

  private[vm] val BufferSize = 1024 * 1024

  private[vm] val byteStringBuffer = new ThreadLocal[ByteBuffer]()

  private[vm] def getByteStringBuffer = {
    val pb = byteStringBuffer.get()
    if (pb == null) {
      val newPb = ByteBuffer.allocate(BufferSize)
      byteStringBuffer.set(newPb)
      newPb
    } else {
      pb.clear()
      pb
    }
  }

  // scalafix:on DisableSyntax.keywords.null

  /** Seek first non zero byte in pb **/
  private[vm] def seek(pb: ByteBuffer): Unit = {
    @tailrec def aux(i: Int): Unit =
      if (i == pb.limit) pb.position(i)
      else if (pb.get(i) == 0) aux(i + 1)
      else pb.position(i)
    aux(0)
  }

  private[vm] def erase(pb: ByteBuffer): Unit = {
    while (pb.hasRemaining) pb.put(0.toByte)
    pb.rewind()
  }

  object parser {

    val (primitive, all, utf8, ref, bytes, bigint, uint, int) = {
      import fastparse.all._

      val ws = P(CharIn(Seq(' ', '\t', '\n', '\r')).rep)
      val comma = P(ws ~ "," ~ ws)
      val decDigs = P(CharIn('0' to '9').rep(1))
      val hexDig = P(CharIn('0' to '9', 'a' to 'f', 'A' to 'F'))
      val hexDigs = P(hexDig.rep(1))

      val float = P("-".!.? ~ decDigs.! ~ "." ~ decDigs.! ~ (CharIn("eE") ~ CharIn("+-").? ~ decDigs.rep).!.?) map {
        case (maybeMinus, integerPart, fractionalPart, maybeExpPart) =>
          val m = maybeMinus.getOrElse("")
          val e = maybeExpPart.getOrElse("")
          s"$m$integerPart.$fractionalPart$e".toDouble
      }

      val uint = {
        val hexInt = P(IgnoreCase("0x") ~/ hexDigs.!).map(s => BigInt(s, 16))
        val decInt = P(decDigs.!).map(BigInt.apply)
        P(hexInt | decInt)
      }

      val int = P("-".!.? ~ uint) map {
        case (m, i) => i * m.fold(1)(_ => -1)
      }

      val int8 = P(IgnoreCase("int8(") ~/ ws ~ int ~ ws ~ ")").map(x => Primitive.Int8(x.toByte))
      val int16 = P(IgnoreCase("int16(") ~/ ws ~ int ~ ws ~ ")").map(x => Primitive.Int16(x.toShort))
      val int32 = P(IgnoreCase("int32(") ~/ ws ~ int ~ ws ~ ")").map(x => Primitive.Int32(x.toInt))
      val uint8 = P(IgnoreCase("uint8(") ~/ ws ~ uint ~ ws ~ ")").map(x => Primitive.Uint8(x.toInt))
      val uint16 = P(IgnoreCase("uint16(") ~/ ws ~ uint ~ ws ~ ")").map(x => Primitive.Uint16(x.toInt))
      val uint32 = P(IgnoreCase("uint32(") ~/ ws ~ uint ~ ws ~ ")").map(x => Primitive.Uint32(x.toLong))
      val bigint = P(IgnoreCase("bigint(") ~/ ws ~ int ~ ws ~ ")").map(x => Primitive.BigInt(x))
      val number = P(IgnoreCase("number(") ~/ ws ~ float ~ ws ~ ")").map(x => Primitive.Number(x))

      val inferredNumeric = P {
        float.map(Primitive.Number) | int.map {
          case v if v >= 0 && v <= 0xFF                                    => Primitive.Uint8(v.toInt)
          case v if v >= 0 && v <= 0xFFFF                                  => Primitive.Uint16(v.toInt)
          case v if v >= 0 && v <= 0xFFFFFFFFL                             => Primitive.Uint32(v.toLong)
          case v if v >= Byte.MinValue.toInt && v <= Byte.MaxValue.toInt   => Primitive.Int8(v.toByte)
          case v if v >= Short.MinValue.toInt && v <= Short.MaxValue.toInt => Primitive.Int16(v.toShort)
          case v if v >= Int.MinValue && v <= Int.MaxValue                 => Primitive.Int32(v.toInt)
          case v                                                           => Primitive.BigInt(v.toLong)
        }
      }

      val numeric = P(number | int8 | int16 | int32 | uint8 | uint16 | uint32 | bigint | inferredNumeric)

      val bool = P(
        IgnoreCase("true").map(_ => Primitive.Bool.True) | IgnoreCase("false").map(_ => Primitive.Bool.False))

      val ref = P("#" ~ uint).map(_.toInt).map(Primitive.Ref.apply)

      val `null` = P(IgnoreCase("null")).map(_ => Primitive.Null)

      val string = {
        val unicodeEscape = P("u" ~ (hexDig ~ hexDig ~ hexDig ~ hexDig).!)
          .map(s => new String(Character.toChars(Integer.parseInt(s, 16))))
        val special = P(CharIn("\"bfnrt\\").!) map {
          case "b" => "\b"
          case "f" => "\f"
          case "n" => "\n"
          case "r" => "\r"
          case "t" => "\t"
          case x   => x
        }
        val escape = P("\\" ~/ (special | unicodeEscape))
        val strChars = P(CharsWhile(c => c != '"' && c != '\\').!)
        P("\"" ~/ (strChars | escape).rep ~ "\"").map(_.mkString)
      }

      val utf8 = string.map(Primitive.Utf8)

      val hexString = P(hexDigs.!).map(s => hex2byteString(s))

      val bytes = P(IgnoreCase("x") ~ hexString).map(Primitive.Bytes)

      val primitive: Parser[Primitive] = P(utf8 | bytes | bool | ref | numeric | `null`)

      def arrayParser[P1, P2, T, A](prefix: String, p1: Parser[P1], p2: Parser[P2])(array: mutable.Buffer[T] => A,
                                                                                    f1: P1 => T,
                                                                                    f2: P2 => T): Parser[A] = {
        P(IgnoreCase(s"$prefix[") ~ ws ~ p1.rep(sep = comma) ~ ws ~ "]").map(xs => array(xs.map(f1).toBuffer)) |
          P("[" ~ p2.rep(sep = comma, min = 1) ~ "]").map(xs => array(xs.map(f2).toBuffer))
      }

      val int8Array = arrayParser("int8", int, int8)(Array.Int8Array, _.toByte, _.data)
      val int16Array = arrayParser("int16", int, int16)(Array.Int16Array, _.toShort, _.data)
      val int32Array = arrayParser("int32", int, int32)(Array.Int32Array, _.toInt, _.data)
      val uint8Array = arrayParser("uint8", uint, uint8)(Array.Uint8Array, _.toInt, _.data)
      val uint16Array = arrayParser("uint16", uint, uint16)(Array.Uint16Array, _.toInt, _.data)
      val uint32Array = arrayParser("uint32", uint, uint32)(Array.Uint32Array, _.toLong, _.data)
      val numberArray = arrayParser("number", float, number)(Array.NumberArray, identity, _.data)
      val bigintArray = arrayParser("bigint", int, bigint)(Array.BigIntArray, identity, _.data)
      val boolArray = arrayParser("bool", bool, bool)(Array.BoolArray, identity, identity)
      val refArray = arrayParser("#", uint, ref)(Array.RefArray, _.toInt, _.data)
      val utf8Array = arrayParser("utf8", string, utf8)(Array.Utf8Array, identity, _.data)
      val bytesArray = arrayParser("x", hexString, bytes)(Array.BytesArray, identity, _.data)

      val array = P(
        int8Array | int16Array | int32Array
          | uint8Array | uint16Array | uint32Array
          | bigintArray | numberArray | refArray
          | boolArray | utf8Array | bytesArray
      )

      val struct = P("{" ~/ ws ~ (primitive ~ ws ~ ":" ~ ws ~ primitive).rep(sep = comma) ~ ws ~ "}")
        .map(xs => Struct(mutable.Map(xs: _*)))

      val all = P(struct | array | primitive)

      // exports
      (primitive, all, utf8, ref, bytes, bigint, uint, int)
    }
  }

  def fromByteString(byteString: ByteString, offset: Int = 0): (Int, Data) = {
    val buffer = byteString.asReadOnlyByteBuffer()
    buffer.position(offset)
    val result = readFromByteBuffer(buffer)
    (buffer.position, result)
  }

  def fromBytes(bytes: scala.Array[Byte]): Data = {
    val buffer = getByteStringBuffer
    buffer.put(bytes)
    buffer.flip()
    readFromByteBuffer(buffer)
  }

  def readFromByteBuffer(buffer: ByteBuffer): Data = {

    // Gets dynamic length of subsequent byte string.
    // Positive result means length.
    // Negative result means that length is 0 and length-byte contains data
    def getLength: Int = {
      val first = buffer.get & 0xFF
      val lenlen = (first & 0xC0) >> 6
      val rest = first & 0x3F
      //println(s"getLength: first = $first, lenlen = $lenlen, rest = $rest")
      (lenlen: @switch) match {
        case 0 => -rest
        case 1 => rest.toInt
        case 2 => (rest << 8 | (buffer.get & 0xFF)) & 0xFFFF
        case 3 => (rest << 16 | (buffer.get & 0xFF) << 8 | (buffer.get & 0xFF)) & 0xFFFFFF
      }
    }

    def primitiveBuffer(alignment: Int): ByteBuffer = {
      val pb = getPrimitiveBuffer
      pb.limit(alignment)
      pb.rewind()
      erase(pb)
      @tailrec def pad(i: Int): Unit = (i: @switch) match {
        case 0 => ()
        case _ =>
          pb.put(0.toByte)
          pad(i - 1)
      }
      val length = getLength
      if (length < 0) {
        pad(alignment - 1)
        pb.put((length * -1).toByte)
      } else {
        //println(s"primitiveBuffer: length = $length, alignment = $alignment")
        val dl = alignment - length
        if (dl < 0)
          throw UnexpectedLengthException(alignment.toString, length, buffer.position())
        pad(dl)
        //println(s"primitiveBuffer: pb = $pb, buffer = $buffer")
        while (pb.hasRemaining) pb.put(buffer.get)
      }
      pb.rewind()
      pb
    }

    // Read [[length]] subsequent bytes from buffer
    def getBytes(length: Int) =
      if (length < 0) {
        val bytes = new ScalaArray[Byte](1)
        bytes(0) = (length * -1).toByte
        bytes
      } else {
        val bytes = new ScalaArray[Byte](length)
        buffer.get(bytes)
        bytes
      }

    def getByteString =
      ByteString.copyFrom(getBytes(getLength))

    def getString = new String(getBytes(getLength))

    def getNestedPrimitive = {
      val offset = buffer.position()
      readFromByteBuffer(buffer) match {
        case x: Primitive => x
        case x            => throw UnexpectedTypeException(x.getClass, offset)
      }
    }

    def getInt8 = primitiveBuffer(1).get
    def getBool: Primitive.Bool =
      if (primitiveBuffer(1).get > 0) Primitive.Bool.True
      else Primitive.Bool.False
    def getInt16 = primitiveBuffer(2).getShort
    def getInt32 = primitiveBuffer(4).getInt
    def getUint8 = primitiveBuffer(4).getInt & 0xFF
    def getUint16 = primitiveBuffer(4).getInt & 0xFFFF
    def getUint32: Long = primitiveBuffer(8).getLong & 0xFFFFFFFFl
    def getBigInt = scala.BigInt(getBytes(getLength))
    def getDouble = primitiveBuffer(8).getDouble
    def getRef = buffer.getInt()

    buffer.get match {
      case Type.Null    => Primitive.Null
      case Type.Int8    => Primitive.Int8(getInt8) // int8
      case Type.Int16   => Primitive.Int16(getInt16) // int16
      case Type.Int32   => Primitive.Int32(getInt32) // int32
      case Type.Uint8   => Primitive.Uint8(getUint8) // uint8
      case Type.Uint16  => Primitive.Uint16(getUint16) // uint16
      case Type.Uint32  => Primitive.Uint32(getUint32) // uint32
      case Type.BigInt  => Primitive.BigInt(getBigInt) // uint64
      case Type.Number  => Primitive.Number(getDouble) // decimal // TODO
      case Type.Boolean => getBool
      case Type.Ref     => Primitive.Ref(getRef)
      case Type.Utf8    => Primitive.Utf8(getString) // utf8
      case Type.Bytes   => Primitive.Bytes(getByteString) // bytes
      case Type.Array =>
        val `type` = buffer.get
        val l = getLength
        //println(s"l=$l")
        if (l < 0) throw UnexpectedLengthException("greater than 0", l, buffer.position - 1)
        `type` match {
          case Type.Int8    => Array.Int8Array(mutable.Buffer.fill(l)(getInt8))
          case Type.Int16   => Array.Int16Array(mutable.Buffer.fill(l)(getInt16))
          case Type.Int32   => Array.Int32Array(mutable.Buffer.fill(l)(getInt32))
          case Type.BigInt  => Array.BigIntArray(mutable.Buffer.fill(l)(getBigInt))
          case Type.Uint8   => Array.Uint8Array(mutable.Buffer.fill(l)(getUint8))
          case Type.Uint16  => Array.Uint16Array(mutable.Buffer.fill(l)(getUint16))
          case Type.Uint32  => Array.Uint32Array(mutable.Buffer.fill(l)(getUint32))
          case Type.Number  => Array.NumberArray(mutable.Buffer.fill(l)(getDouble))
          case Type.Boolean => Array.BoolArray(mutable.Buffer.fill(l)(getBool))
          case Type.Ref     => Array.RefArray(mutable.Buffer.fill(l)(getRef))
          case Type.Utf8    => Array.Utf8Array(mutable.Buffer.fill(l)(getString))
          case Type.Bytes   => Array.BytesArray(mutable.Buffer.fill(l)(getByteString))
        }
      case Type.Struct =>
        val l = getLength
        if (l < 0) throw UnexpectedLengthException("greater than 0", l, buffer.position - 1)
        val data = mutable.Map(Seq.fill(l)(getNestedPrimitive -> getNestedPrimitive): _*)
        Struct(data)
    }
  }

  final case class UnknownTypeException(typeTag: Type, offset: Int)
      extends Exception(s"Unknown type: $typeTag at $offset")

  final case class UnexpectedTypeException(`type`: Class[_ <: Data], offset: Int)
      extends Exception(s"Unexpected type: ${`type`.getSimpleName} at $offset")

  final case class UnexpectedLengthException(expected: String, given: Int, offset: Int)
      extends Exception(s"Unexpected length: $expected expected but $given given at $offset")

  object Type extends TaggedType[Byte] {
    final val Null = Type @@ 0x00.toByte
    final val Int8 = Type @@ 0x01.toByte
    final val Int16 = Type @@ 0x02.toByte
    final val Int32 = Type @@ 0x03.toByte
    final val BigInt = Type @@ 0x04.toByte
    final val Uint8 = Type @@ 0x05.toByte
    final val Uint16 = Type @@ 0x06.toByte
    final val Uint32 = Type @@ 0x07.toByte
    final val Number = Type @@ 0x08.toByte
    final val Boolean = Type @@ 0x09.toByte
    final val Ref = Type @@ 0x0A.toByte
    final val Utf8 = Type @@ 0x0B.toByte
    final val Array = Type @@ 0x0C.toByte
    final val Struct = Type @@ 0x0D.toByte
    final val Bytes = Type @@ 0x0E.toByte
  }

  type Type = Type.Type
}
