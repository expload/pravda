package pravda.vm

import java.nio.ByteBuffer

import org.scalacheck._
import Arbitrary.arbitrary
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

import scala.annotation.strictfp
import scala.collection.mutable

@strictfp object DataSpecification extends Properties("Data") {

  import Data._

  def genPrimitive[T, P, A](gen: Gen[T], p: T => P, a: mutable.Buffer[T] => A): (Gen[P], Gen[A]) =
    (gen.map(p), Gen.containerOf[mutable.Buffer, T](gen).map(a))

  val (int8, int8Array) = genPrimitive(arbitrary[Byte], Primitive.Int8, Int8Array)
  val (int16, int16Array) = genPrimitive(arbitrary[Short], Primitive.Int16, Int16Array)
  val (int32, int32Array) = genPrimitive(arbitrary[Int], Primitive.Int32, Int32Array)

  val (uint8, uint8Array) = genPrimitive(arbitrary[Int].suchThat(x => x >= 0 && x <= 0xFF), Primitive.Uint8, Uint8Array)
  val (uint16, uint16Array) = genPrimitive(arbitrary[Int].suchThat(x => x >= 0 && x <= 0xFFFF), Primitive.Uint16, Uint16Array)
  val (uint32, uint32Array) = genPrimitive(arbitrary[Long].suchThat(x => x >= 0 && x <= 0xFFFFFFFFl), Primitive.Uint32, Uint32Array)

  val (bigInt, bigIntArray) = genPrimitive(arbitrary[BigInt], Primitive.BigInt, BigIntArray)
  val (number, numberArray) = genPrimitive(arbitrary[Double], Primitive.Number, NumberArray)

  val (ref, refArray) = genPrimitive(arbitrary[Int], Primitive.Ref, RefArray)
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

  implicit val primitive: Gen[Primitive] = Gen.oneOf(
    int8, int16, int32, boolean, number,
    uint8, uint16, uint32, bigInt, ref
  )

  val utf8: Gen[Utf8] = arbitrary[String].map(Utf8)


  val struct: Gen[Struct] = {
    for {
      name <- arbitrary[String]
      recordGen = arbitrary[String].flatMap(field => primitive.map(value => (field, value)))
      fields <- Gen.containerOf[Seq, (String, Primitive)](recordGen).map(xs => mutable.SortedMap(xs:_*))
    } yield Struct(name, fields)
  }

  val data: Gen[Data] = Gen.oneOf(
    utf8, primitive, struct,
    int8Array, int16Array, int32Array, numberArray,
    uint8Array, uint16Array, uint32Array, bigIntArray
  )

  property("writeToByteBuffer -> readFromByteBuffer") = forAll(data) { data =>
    val buffer = ByteBuffer.allocate(64 * 1024)
    data.writeToByteBuffer(buffer)
    buffer.flip()
    Data.readFromByteBuffer(buffer) == data
  }
}