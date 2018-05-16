package io.mytc.sood.vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import state.{Address, Data}

package object serialization {

  def dataToInt32(data: Data): Int = {
    data.asReadOnlyByteBuffer().getInt
  }

  def dataToDouble(data: Data): Double = {
    data.asReadOnlyByteBuffer().getDouble
  }

  def int32ToByteString(i: Int): ByteString = {
    val buf = ByteBuffer.allocate(4).putInt(i)
    buf.rewind()
    ByteString.copyFrom(buf)
  }

  def int32ToData(i: Int): Data =
    int32ToByteString(i)

  def int32ToAddress(i: Int): Address =
    int32ToByteString(i)

  def doubleToData(v: Double): Data = {
    val buf = ByteBuffer.allocate(8)
    buf.putDouble(v)
    buf.rewind()
    ByteString.copyFrom(buf)
  }

  def wordToData(source: ByteBuffer): Data = {
    ByteString.copyFrom(wordToBytes(source))
  }

  val FALSE: Data = ByteString.copyFrom(Array[Byte](0))
  val TRUE: Data = ByteString.copyFrom(Array[Byte](1))

  def boolToData(b: Boolean): Data = {
    if (b) TRUE else FALSE
  }

  def dataToBool(d: Data): Boolean = {
    import scala.collection.JavaConverters._
    d.iterator().asScala.exists(_ != 0.toByte)
  }

  def bytesToByteString(ints: Int*): ByteString =
    ByteString.copyFrom(ints.map(_.toByte).toArray)

  def bytesToData(ints: Int*): Data =
    bytesToByteString(ints: _*)

  def bytesToAddress(ints: Int*): Address =
    bytesToByteString(ints: _*)
}
