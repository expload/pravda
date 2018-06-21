package pravda.vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString

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
      case Struct(name, data) =>
        buffer.put(TypeStruct.toByte)
        putString(name)
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

    sealed trait Numeric extends Primitive

    final case class Int8(data: Byte)               extends Numeric
    final case class Int16(data: Short)             extends Numeric
    final case class Int32(data: Int)               extends Numeric
    final case class Uint8(data: Int)               extends Numeric
    final case class Uint16(data: Int)              extends Numeric
    final case class Uint32(data: Long)             extends Numeric
    final case class BigInt(data: ScalaBigInt)      extends Numeric
    final case class Number(data: Double)           extends Numeric
    final case class Ref(data: Int)                 extends Primitive
    case object Null                                extends Primitive

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

  final case class Struct(name: String, data: mutable.SortedMap[String, Primitive]) extends Data
  final case class Utf8(data: String)                                               extends Array
  final case class MarshalledData(data: ByteString)                                 extends Data

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
        val name = getString
        val l = getLength
        if (l < 0) throw UnexpectedLengthException("greater than 0", l, buffer.position - 1)
        val data = mutable.SortedMap(Seq.fill(l)(getString -> getNestedPrimitive): _*)
        Struct(name, data)
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
