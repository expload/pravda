package pravda.vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.bytes

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

    def ref(x: Int): String =
      (x & 0xFFFFFFFFl).toString

    this match {
      // Primitives
      case numeric: Primitive.Numeric[_] if untypedNumerics =>
        numeric.data.toString
      case Int8(data)   => s"int8($data)"
      case Int16(data)  => s"int16($data)"
      case Int32(data)  => s"int32($data)"
      case Uint8(data)  => s"uint8($data)"
      case Uint16(data) => s"uint16($data)"
      case Uint32(data) => s"uint32($data)"
      case Number(data) => s"number($data)"
      case BigInt(data) => s"bigint($data)"
      case Ref(data)    => s"#${ref(data)}"
      case Bool.True    => "true"
      case Bool.False   => "false"
      // Arrays
      case Int8Array(data)   => s"int8[${data.mkString(comma)}]"
      case Int16Array(data)  => s"int16[${data.mkString(comma)}]"
      case Int32Array(data)  => s"int32[${data.mkString(comma)}]"
      case Uint8Array(data)  => s"uint8[${data.mkString(comma)}]"
      case Uint16Array(data) => s"uint16[${data.mkString(comma)}]"
      case Uint32Array(data) => s"uint32[${data.mkString(comma)}]"
      case NumberArray(data) => s"number[${data.mkString(comma)}]"
      case BigIntArray(data) => s"bigint[${data.mkString(comma)}]"
      case RefArray(data)    => s"#[${data.map(ref).mkString(comma)}]"
      case Utf8(data)        => s""""${escape(data)}""""
      case Null              => "null"
      case BoolArray(data) =>
        val xs = data map {
          case Bool.True  => "true"
          case Bool.False => "false"
        }
        s"bool[${xs.mkString(comma)}]"
      case Struct(data) if data.isEmpty => "{}"
      case Struct(data) =>
        val commaNl = if (pretty) ",\n" else ","
        val xs = data map {
          case (k, v) =>
            val pk = escape(k)
            val pv = v.mkString(untypedNumerics, escapeUnicode, pretty)
            if (pretty) s"""  "$pk": $pv"""
            else s""""$pk":$pv"""
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

    def putString(data: String) = {
      val bytes = data.getBytes
      putLength(buffer, bytes.length)
      buffer.put(bytes)
    }

    def putPrimitive(typeTag: Byte)(f: ByteBuffer => Unit): Unit = {
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

    def putPrimitiveArray[T, U](typeTag: Byte, xs: Seq[T])(f: (ByteBuffer, T) => U): Unit = {
      buffer.put(TypeArray.toByte)
      buffer.put(typeTag)
      putLength(buffer, xs.length)
      xs.foreach(x => putTaglessPrimitive(f(_, x)))
    }

    def putArray[T, U](typeTag: Byte, xs: Seq[T])(f: (ByteBuffer, T) => U): Unit = {
      buffer.put(TypeArray.toByte)
      buffer.put(typeTag)
      putLength(buffer, xs.length)
      xs.foreach(x => f(buffer, x))
    }

    def putTaggedBigInt(buffer: ByteBuffer, value: ScalaBigInt): Unit = {
      buffer.put(TypeBigInt.toByte)
      putBigInt(buffer, value)
    }

    def putBigInt(buffer: ByteBuffer, value: ScalaBigInt): Unit = {
      val bytes = value.toByteArray
      putLength(buffer, bytes.length)
      buffer.put(bytes)
    }

    def putBoolean(buffer: ByteBuffer, value: Byte) = {
      buffer.put(TypeBoolean)
      buffer.put(value)
    }

    this match {
      case Null              => buffer.put(TypeNull)
      case Int8(data)        => putPrimitive(TypeInt8)(_.put(data))
      case Int16(data)       => putPrimitive(TypeInt16)(_.putShort(data))
      case Int32(data)       => putPrimitive(TypeInt32)(_.putInt(data))
      case Uint8(data)       => putPrimitive(TypeUint8)(_.putInt(data))
      case Uint16(data)      => putPrimitive(TypeUint16)(_.putInt(data))
      case Uint32(data)      => putPrimitive(TypeUint32)(_.putLong(data))
      case BigInt(data)      => putTaggedBigInt(buffer, data)
      case Ref(data)         => putPrimitive(TypeRef.toByte)(_.putInt(data))
      case Bool.True         => putBoolean(buffer, 1.toByte)
      case Bool.False        => putBoolean(buffer, 0.toByte)
      case Int8Array(data)   => putPrimitiveArray(TypeInt8, data)(_.put(_))
      case Int16Array(data)  => putPrimitiveArray(TypeInt16, data)(_.putShort(_))
      case Int32Array(data)  => putPrimitiveArray(TypeInt32, data)(_.putInt(_))
      case Uint8Array(data)  => putPrimitiveArray(TypeUint8, data)(_.putInt(_))
      case Uint16Array(data) => putPrimitiveArray(TypeUint16, data)(_.putInt(_))
      case Uint32Array(data) => putPrimitiveArray(TypeUint32, data)(_.putLong(_))
      case BigIntArray(data) => putArray(TypeBigInt, data)(putBigInt)
      case RefArray(data)    => putPrimitiveArray(TypeRef, data)(_.putInt(_))
      case Number(data)      => putPrimitive(TypeNumber)(_.putDouble(data))
      case NumberArray(data) => putPrimitiveArray(TypeNumber, data)(_.putDouble(_))
      case BoolArray(data) =>
        putPrimitiveArray(TypeBoolean.toByte, data) { (b, x) =>
          if (x == Bool.True) b.put(1.toByte)
          else b.put(0.toByte)
        }
      case Utf8(data) =>
        buffer.put(TypeUtf8.toByte)
        putString(data)
      case Struct(data) =>
        buffer.put(TypeStruct.toByte)
        putLength(buffer, data.size)
        data.foreach {
          case (field, value) =>
            putString(field)
            value.writeToByteBuffer(buffer)
        }
      case MarshalledData(data) =>
        buffer.put(data.toByteArray)
    }
  }

}

@strictfp object Data {

  sealed trait Primitive extends Data

  object Primitive {

    sealed abstract class Numeric[T: scala.Numeric] extends Primitive {
      def data: T
    }

    final case class Int8(data: Byte)           extends Numeric[Byte]
    final case class Int16(data: Short)         extends Numeric[Short]
    final case class Int32(data: Int)           extends Numeric[Int]
    final case class Uint8(data: Int)           extends Numeric[Int]
    final case class Uint16(data: Int)          extends Numeric[Int]
    final case class Uint32(data: Long)         extends Numeric[Long]
    final case class BigInt(data: scala.BigInt) extends Numeric[scala.BigInt]
    final case class Number(data: Double)       extends Numeric[Double]
    final case class Ref(data: Int)             extends Primitive
    case object Null                            extends Primitive

    sealed trait Bool extends Primitive

    object Bool {

      def apply(value: Boolean): Bool =
        if (value) True else False
      final case object True  extends Bool
      final case object False extends Bool
    }
  }

  sealed trait Array extends Data

  object Array {
    final case class Int8Array(data: mutable.Buffer[Byte])               extends Array
    final case class Int16Array(data: mutable.Buffer[Short])             extends Array
    final case class Int32Array(data: mutable.Buffer[Int])               extends Array
    final case class Uint8Array(data: mutable.Buffer[Int])               extends Array
    final case class Uint16Array(data: mutable.Buffer[Int])              extends Array
    final case class Uint32Array(data: mutable.Buffer[Long])             extends Array
    final case class BigIntArray(data: mutable.Buffer[ScalaBigInt])      extends Array
    @strictfp final case class NumberArray(data: mutable.Buffer[Double]) extends Array
    final case class RefArray(data: mutable.Buffer[Int])                 extends Array
    final case class BoolArray(data: mutable.Buffer[Primitive.Bool])     extends Array
  }

  final case class Struct(data: mutable.SortedMap[String, Primitive]) extends Data
  final case class Utf8(data: String)                                 extends Array
  final case class MarshalledData(data: ByteString)                   extends Data

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

    val (primitive, all) = {
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
        val hexInt = P("0x" ~/ hexDigs.!).map(s => BigInt(s, 16))
        val decInt = P(decDigs.!).map(BigInt.apply)
        P(hexInt | decInt)
      }

      val int = P("-".!.? ~ uint) map {
        case (m, i) => i * m.fold(1)(_ => -1)
      }

      val int8 = P("int8(" ~/ ws ~ int ~ ws ~ ")").map(x => Primitive.Int8(x.toByte))
      val int16 = P("int16(" ~/ ws ~ int ~ ws ~ ")").map(x => Primitive.Int16(x.toShort))
      val int32 = P("int32(" ~/ ws ~ int ~ ws ~ ")").map(x => Primitive.Int32(x.toInt))
      val uint8 = P("uint8(" ~/ ws ~ uint ~ ws ~ ")").map(x => Primitive.Uint8(x.toInt))
      val uint16 = P("uint16(" ~/ ws ~ uint ~ ws ~ ")").map(x => Primitive.Uint16(x.toInt))
      val uint32 = P("uint32(" ~/ ws ~ uint ~ ws ~ ")").map(x => Primitive.Uint32(x.toLong))
      val bigint = P("bigint(" ~/ ws ~ int ~ ws ~ ")").map(x => Primitive.BigInt(x))
      val number = P("number(" ~/ ws ~ float ~ ws ~ ")").map(x => Primitive.Number(x))

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

      val bool = P("true").map(_ => Primitive.Bool.True) | P("false").map(_ => Primitive.Bool.False)

      val ref = P("#" ~ uint).map(_.toInt).map(Primitive.Ref)

      val `null` = P("null").map(_ => Primitive.Null)

      val primitive: Parser[Primitive] = P(bool | ref | numeric | `null`)

      def arrayParser[P1, P2, T, A](prefix: String, p1: Parser[P1], p2: Parser[P2])(array: mutable.Buffer[T] => A,
                                                                                    f1: P1 => T,
                                                                                    f2: P2 => T): Parser[A] = {
        P(
          P(s"$prefix[" ~ ws ~ p1.rep(sep = comma) ~ ws ~ "]").map(xs => array(xs.map(f1).toBuffer)) |
            P("[" ~ p2.rep(sep = comma, min = 1) ~ "]").map(xs => array(xs.map(f2).toBuffer))
        )
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

      val array = P(
        numberArray | int8Array |
          int16Array | int32Array | uint8Array | uint16Array |
          uint32Array | bigintArray | boolArray | refArray)

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

      val utf8 = string.map(Utf8)

      val byteArray = P("x" ~ hexDigs.!)
        .map(s => Array.Int8Array(bytes.hex2bytes(s).toBuffer))

      val struct = P("{" ~/ ws ~ (string ~ ws ~ ":" ~ ws ~ primitive).rep(sep = comma) ~ ws ~ "}")
        .map(xs => Struct(mutable.SortedMap(xs: _*)))

      val all = P(struct | byteArray | utf8 | array | primitive)

      (primitive, all)
    }
  }

  def fromString(string: String): Data = {
    parser.all.parse(string).get.value
  }

  def fromBytes(bytes: ScalaArray[Byte]): Data = {
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

    def getString = new String(getBytes(getLength))

    def getNestedPrimitive = {
      val offset = buffer.position()
      readFromByteBuffer(buffer) match {
        case x: Primitive => x
        case x            => throw TypeUnexpectedException(x.getClass, offset)
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
    def getBigInt = ScalaBigInt(getBytes(getLength))
    def getDouble = primitiveBuffer(8).getDouble
    def getRef = primitiveBuffer(4).getInt

    buffer.get match {
      case TypeNull    => Primitive.Null
      case TypeInt8    => Primitive.Int8(getInt8) // int8
      case TypeInt16   => Primitive.Int16(getInt16) // int16
      case TypeInt32   => Primitive.Int32(getInt32) // int32
      case TypeUint8   => Primitive.Uint8(getUint8) // uint8
      case TypeUint16  => Primitive.Uint16(getUint16) // uint16
      case TypeUint32  => Primitive.Uint32(getUint32) // uint32
      case TypeBigInt  => Primitive.BigInt(getBigInt) // uint64
      case TypeNumber  => Primitive.Number(getDouble) // decimal // TODO
      case TypeBoolean => getBool
      case TypeRef     => Primitive.Ref(getRef)
      case TypeUtf8    => Utf8(getString) // utf8
      case TypeArray =>
        val `type` = buffer.get
        val l = getLength
        //println(s"l=$l")
        if (l < 0) throw UnexpectedLengthException("greater than 0", l, buffer.position - 1)
        `type` match {
          case TypeInt8    => Array.Int8Array(mutable.Buffer.fill(l)(getInt8))
          case TypeInt16   => Array.Int16Array(mutable.Buffer.fill(l)(getInt16))
          case TypeInt32   => Array.Int32Array(mutable.Buffer.fill(l)(getInt32))
          case TypeBigInt  => Array.BigIntArray(mutable.Buffer.fill(l)(getBigInt))
          case TypeUint8   => Array.Uint8Array(mutable.Buffer.fill(l)(getUint8))
          case TypeUint16  => Array.Uint16Array(mutable.Buffer.fill(l)(getUint16))
          case TypeUint32  => Array.Uint32Array(mutable.Buffer.fill(l)(getUint32))
          case TypeNumber  => Array.NumberArray(mutable.Buffer.fill(l)(getDouble))
          case TypeBoolean => Array.BoolArray(mutable.Buffer.fill(l)(getBool))
          case TypeRef     => Array.RefArray(mutable.Buffer.fill(l)(getRef))
        }
      case TypeStruct =>
        val l = getLength
        if (l < 0) throw UnexpectedLengthException("greater than 0", l, buffer.position - 1)
        val data = mutable.SortedMap(Seq.fill(l)(getString -> getNestedPrimitive): _*)
        Struct(data)
    }
  }

  final case class TypeUnknownException(typeTag: Byte, offset: Int)
      extends Exception(s"Unknown type: $typeTag at $offset")

  final case class TypeUnexpectedException(`type`: Class[_ <: Data], offset: Int)
      extends Exception(s"Unexpected type: ${`type`.getSimpleName} at $offset")

  final case class UnexpectedLengthException(expected: String, given: Int, offset: Int)
      extends Exception(s"Unexpected length: $expected expected but $given given at $offset")

  final val TypeNull = 0x00.toByte
  final val TypeInt8 = 0x01.toByte
  final val TypeInt16 = 0x02.toByte
  final val TypeInt32 = 0x03.toByte
  final val TypeBigInt = 0x04.toByte
  final val TypeUint8 = 0x05.toByte
  final val TypeUint16 = 0x06.toByte
  final val TypeUint32 = 0x07.toByte
  final val TypeNumber = 0x08.toByte
  final val TypeBoolean = 0x09.toByte
  final val TypeRef = 0x0A.toByte
  final val TypeUtf8 = 0x0B.toByte
  final val TypeArray = 0x0C.toByte
  final val TypeStruct = 0x0D.toByte
}
