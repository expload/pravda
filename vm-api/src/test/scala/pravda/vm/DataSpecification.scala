package pravda.vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import org.scalacheck.{Properties, _}

import scala.annotation.strictfp
import scala.collection.mutable

@strictfp object DataSpecification extends Properties("Data") {

  import Data._
  import Data.Array._

  def genPrimitive[T, P, A](gen: Gen[T], p: T => P, a: mutable.Buffer[T] => A): (Gen[P], Gen[A]) =
    (gen.map(p), Gen.containerOf[mutable.Buffer, T](gen).map(a))

  val (int8, int8Array) = genPrimitive(
    Gen.chooseNum[Byte](Byte.MinValue, -1),
    Primitive.Int8, Int8Array)
  val (int16, int16Array) = genPrimitive(
    Gen.chooseNum[Short](Short.MinValue, (Byte.MinValue - 1).toShort),
    Primitive.Int16, Int16Array)
  val (int32, int32Array) = genPrimitive(
    Gen.chooseNum[Int](Int.MinValue, Short.MinValue - 1),
    Primitive.Int32, Int32Array)

  val (uint8, uint8Array) = genPrimitive(Gen.chooseNum(0, 0xFF), Primitive.Uint8, Uint8Array)
  val (uint16, uint16Array) = genPrimitive(Gen.chooseNum(0xFF + 1, 0xFFFF), Primitive.Uint16, Uint16Array)
  val (uint32, uint32Array) = genPrimitive(Gen.chooseNum(0xFFFFl + 1, 0xFFFFFFFFl), Primitive.Uint32, Uint32Array)

  val (bigInt, bigIntArray) = genPrimitive(arbitrary[BigInt].suchThat(x => x < Int.MinValue && x > 0xFFFFFFFFl), Primitive.BigInt, BigIntArray)
  val (number, numberArray) = genPrimitive(arbitrary[Double], Primitive.Number, NumberArray)

  val (ref, refArray) = genPrimitive(arbitrary[Int], Primitive.Ref.apply, RefArray)
  val (boolean, booleanArray) = {
    val f: Boolean => Primitive.Bool = {
      case true => Primitive.Bool.True
      case false => Primitive.Bool.False
    }
    genPrimitive[Boolean, Primitive.Bool, BoolArray](
      arbitrary[Boolean], f,
      array => BoolArray(array.map(f))
    )
  }

  val `null`: Gen[Primitive.Null.type] =
    Gen.const(Data.Primitive.Null)

  val string: Gen[String] = Gen.oneOf(arbitrary[String], Gen.asciiPrintableStr)
  val byteString: Gen[ByteString] = Gen.nonEmptyContainerOf[scala.Array, Byte](arbitrary[Byte]).map(ByteString.copyFrom)
  val bytes: Gen[Primitive.Bytes] = byteString.map(Primitive.Bytes)
  val utf8: Gen[Primitive.Utf8] = string.map(Primitive.Utf8)
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