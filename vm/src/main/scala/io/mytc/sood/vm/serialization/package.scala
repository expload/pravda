package io.mytc.sood.vm

import java.nio.ByteBuffer

import io.mytc.sood.vm.state.Data
import scodec.bits.ByteVector

package object serialization {

  def dataToInt32(data: Data): Int = {
    data.toInt()
  }

  def dataToDouble(data: Data): Double = {
    data.toByteBuffer.getDouble
  }

  def int32ToData(i: Int): Data = {
    ByteVector.fromInt(i)
  }

  def doubleToData(v: Double): Data = {
    val buf = ByteBuffer.allocate(8)
    buf.putDouble(v)
    buf.rewind()
    ByteVector(buf)
  }

  def wordToData(source: ByteBuffer): Data = {
    ByteVector(wordToBytes(source))
  }

  val FALSE: Data = ByteVector(0)
  val TRUE: Data = ByteVector(1)

  def boolToData(b: Boolean): Data = {
    if(b) TRUE else FALSE
  }

  def dataToBool(d: Data): Boolean = {
    d.reverse.toIterable.exists(_ != 0.toByte)
  }

}
