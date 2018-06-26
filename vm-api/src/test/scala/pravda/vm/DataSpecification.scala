package pravda.vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.{BooleanOperators, forAll}
import org.scalacheck.{Properties, _}

import scala.annotation.strictfp
import scala.collection.mutable

@strictfp object DataSpecification extends Properties("Data") {

  import Data._
  import Primitive._
  import Array._

  def genPrimitive[T, P, A](gen: Gen[T], p: T => P, a: mutable.Buffer[T] => A): (Gen[P], Gen[A]) =
    (gen.map(p), Gen.containerOf[mutable.Buffer, T](gen).map(a))

  val (int8, int8Array) = genPrimitive(
    Gen.chooseNum[Byte](Byte.MinValue, -1),
    Int8, Int8Array)
  val (int16, int16Array) = genPrimitive(
    Gen.chooseNum[Short](Short.MinValue, (Byte.MinValue - 1).toShort),
    Int16, Int16Array)
  val (int32, int32Array) = genPrimitive(
    Gen.chooseNum[Int](Int.MinValue, Short.MinValue - 1),
    Int32, Int32Array)

  val (uint8, uint8Array) = genPrimitive(Gen.chooseNum(0, 0xFF), Uint8, Uint8Array)
  val (uint16, uint16Array) = genPrimitive(Gen.chooseNum(0xFF + 1, 0xFFFF), Uint16, Uint16Array)
  val (uint32, uint32Array) = genPrimitive(Gen.chooseNum(0xFFFFl + 1, 0xFFFFFFFFl), Uint32, Uint32Array)

  val (bigInt, bigIntArray) = genPrimitive(arbitrary[scala.BigInt].suchThat(x => x < Int.MinValue && x > 0xFFFFFFFFl), BigInt, BigIntArray)
  val (number, numberArray) = genPrimitive(arbitrary[Double], Number, NumberArray)

  val (ref, refArray) = genPrimitive(arbitrary[Int], Ref.apply, RefArray)
  val (boolean, booleanArray) = {
    val f: Boolean => Bool = {
      case true => Bool.True
      case false => Bool.False
    }
    genPrimitive[Boolean, Bool, BoolArray](
      arbitrary[Boolean], f,
      array => BoolArray(array.map(f))
    )
  }

  val `null`: Gen[Null.type] =
    Gen.const(Null)

  val string: Gen[String] = Gen.oneOf(arbitrary[String], Gen.asciiPrintableStr)
  val byteString: Gen[ByteString] = Gen.nonEmptyContainerOf[scala.Array, Byte](arbitrary[Byte]).map(ByteString.copyFrom)
  val bytes: Gen[Bytes] = byteString.map(Bytes)
  val utf8: Gen[Utf8] = string.map(Utf8)
  val utf8Array: Gen[Utf8Array] = Gen.containerOf[mutable.Buffer, String](string).map(Array.Utf8Array)
  val bytesArray: Gen[BytesArray] = Gen.containerOf[mutable.Buffer, ByteString](byteString).map(Array.BytesArray)

  val primitive: Gen[Primitive] = Gen.oneOf(
    int8, int16, int32, utf8,
    uint8, uint16, uint32,
    bigInt, number, bytes,
    boolean, ref, `null`
  )

  val struct: Gen[Struct] = {
    val recordGen = primitive.flatMap(k => primitive.map(v => (k, v)))
    Gen.containerOf[Seq, (Primitive, Primitive)](recordGen).map(xs => mutable.Map(xs:_*)) map { records =>
      Struct(records)
    }
  }

  val array: Gen[Array] = Gen.oneOf(
    int8Array, int16Array, int32Array,
    uint8Array, uint16Array, uint32Array,
    bigIntArray, numberArray, bytesArray,
    refArray, booleanArray, utf8Array
  )

  val data: Gen[Data] = Gen.oneOf(
    primitive, array, struct
  )

  def castProperty(gen: Gen[Primitive],
                   `type`: Type,
                   toInt8: Boolean = true,
                   toInt16: Boolean = true,
                   toInt32: Boolean = true,
                   toUint8: Boolean = true,
                   toUint16: Boolean = true,
                   toUint32: Boolean = true,
                   toBigInt: Boolean = true,
                   toNumber: Boolean = true,
                   toRef: Boolean = true,
                   toUtf8: Boolean = true,
                   toBytes: Boolean = true): Prop = forAll(gen) { data =>
    (!toInt8 || data.cast(Type.Int8).cast(`type`) == data) :| "to int8" &&
    (!toInt16 || data.cast(Type.Int16).cast(`type`) == data) :| "to int16" &&
    (!toInt32 || data.cast(Type.Int32).cast(`type`) == data) :| "to int32" &&
    (!toUint8 || data.cast(Type.Uint8).cast(`type`) == data) :| "to uint8" &&
    (!toUint16 || data.cast(Type.Uint16).cast(`type`) == data) :| "to uint16" &&
    (!toUint32 || data.cast(Type.Uint32).cast(`type`) == data) :| "to uint32" &&
    (!toBigInt || data.cast(Type.BigInt).cast(`type`) == data) :| "to uint32" &&
    (!toNumber || data.cast(Type.Number).cast(`type`) == data) :| "to number" &&
    (!toRef || data.cast(Type.Ref).cast(`type`) == data) :| "to ref" &&
    (!toUtf8 || data.cast(Type.Utf8).cast(`type`) == data) :| "to utf8" &&
    (!toBytes || data.cast(Type.Bytes).cast(`type`) == data) :| "to bytes"
  }

  property("int8.cast") = castProperty(int8, Type.Int8)
  property("int16.cast") = castProperty(int16, Type.Int16, toInt8 = false, toUint8 = false)
  property("int32.cast") = castProperty(int32, Type.Int32, toInt8 = false, toUint8 = false, toInt16 = false, toUint16 = false)
  property("uint8.cast") = castProperty(uint8, Type.Uint8)
  property("uint16.cast") = castProperty(uint16, Type.Uint16, toInt8 = false, toUint8 = false)
  property("uint32.cast") = castProperty(uint32, Type.Uint32, toInt8 = false, toUint8 = false, toInt16 = false, toUint16 = false)
  property("utf8.cast") = castProperty(Gen.choose(0, 127).map(x => Utf8(x.toString)), Type.Utf8, toNumber = false)
  property("ref.cast") = castProperty(Gen.choose(0, 127).map(x => Ref(x)), Type.Ref)

  property("mkString -> fromString") = forAll(data) { data =>
    val text = data.mkString()
    Data.parser.all.parse(text).get.value == data
  }

  property("mkString(pretty = true) -> fromString") = forAll(data) { data =>
    val text = data.mkString(pretty = true)
    Data.parser.all.parse(text).get.value == data
  }

  property("mkString(untypedNumerics = true) -> fromString") = forAll(data) { data =>
    val text = data.mkString(untypedNumerics = true)
    Data.parser.all.parse(text).get.value == data
  }

  property("mkString(escapeUnicode = true) -> fromString") = forAll(data) { data =>
    val text = data.mkString(escapeUnicode = true)
    Data.parser.all.parse(text).get.value == data
  }

  property("writeToByteBuffer -> readFromByteBuffer") = forAll(data) { data =>
    val buffer = ByteBuffer.allocate(1024 * 1024)
    data.writeToByteBuffer(buffer)
    buffer.flip()
    Data.readFromByteBuffer(buffer) == data
  }
}