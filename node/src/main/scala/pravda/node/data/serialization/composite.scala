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

package pravda.node.data.serialization

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import com.google.protobuf.ByteString
import pravda.common.bytes._
import shapeless.{::, HList, HNil, Lazy}
import supertagged.{Tagged, lifterF}

object composite extends CompositeTranscoder

trait CompositeTranscoder {
  // --- MAIN PART -------------
  type CompositeEncoder[T] = Transcoder[T, Composite]

  def toBytes(str: String): Array[Byte] = str.getBytes(StandardCharsets.UTF_8)

  val splitter: Array[Byte] = toBytes(":")

  // TODO: it doesn't work with partial unification for some reason
  //  implicit def writeGen[T, R <: HList](implicit gen: Generic.Aux[T, R], wr: Lazy[KeyWriter[R]]) = new KeyWriter[T] {
  //    override def toBytes(value: T): Array[Byte] = wr.value.toBytes(gen.to(value))
  //  }

  implicit val hnilEncoder: CompositeEncoder[HNil] = _ => Composite @@ Array.empty[Byte]

  implicit def hlist1Encoder[H](implicit hPrint: Lazy[CompositeEncoder[H]]): CompositeEncoder[H :: HNil] = {
    (l: H :: HNil) =>
      hPrint.value(l.head)
  }

  implicit def hlistEncoder[H, H2, T <: HList](implicit hPrint: Lazy[CompositeEncoder[H]],
                                               h2Print: Lazy[CompositeEncoder[H2]],
                                               tPrint: CompositeEncoder[T]): CompositeEncoder[H :: H2 :: T] = {
    (l: H :: H2 :: T) =>
      l match {
        case h :: h2 :: HNil => Composite @@ (hPrint.value(h) ++ splitter ++ h2Print.value(h2))
        case h :: h2 :: t    => Composite @@ (hPrint.value(h) ++ splitter ++ h2Print.value(h2) ++ splitter ++ tPrint(t))
      }
  }

  implicit val stringWriter: CompositeEncoder[String] = Composite @@ toBytes(_)

  implicit val bytesWrite: CompositeEncoder[Array[Byte]] = (arr: Array[Byte]) => Composite @@ toBytes(bytes2hex(arr))

  implicit val byteWrite: CompositeEncoder[Byte] = (b: Byte) => Composite @@ toBytes(bytes2hex(Array(b)))

  implicit val longWriter: CompositeEncoder[Long] = v => {
    val b = ByteBuffer.allocate(8)
    Composite @@ b.putLong(v).array()
  }

  implicit val intWriter: CompositeEncoder[Int] = v => {
    val b = ByteBuffer.allocate(4)
    Composite @@ b.putInt(v).array()
  }

  implicit val doubleWriter: CompositeEncoder[Double] = v => {
    val b = ByteBuffer.allocate(8)
    Composite @@ b.putDouble(v).array()
  }

  implicit val booleanWriter: CompositeEncoder[Boolean] = v =>
    Composite @@ Array(
      if (v) 0.toByte else 1.toByte
  )

  implicit val bigDecimalWriter: CompositeEncoder[BigDecimal] = v => stringWriter(v.toString)
  //------------------------

  // Tagged types
  implicit def taggedStrWriterLifter[T: CompositeEncoder, U]: CompositeEncoder[Tagged[T, U]] =
    lifterF[CompositeEncoder].lift[T, U]

  implicit val byteStrWrite: CompositeEncoder[ByteString] = { (value: ByteString) =>
    bytesWrite(value.toByteArray)
  }

}
