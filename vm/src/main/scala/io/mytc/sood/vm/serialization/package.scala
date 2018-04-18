package io.mytc.sood.vm

import java.nio.ByteBuffer

import io.mytc.sood.vm.state.Data

package object serialization {

  def dataToInt32(data: Data): Int = {
    ByteBuffer.wrap(data).getInt
  }

  def int32ToData(i: Int): Data = {
    val buf = ByteBuffer.allocate(4)
    buf.putInt(i)
    buf.array()
  }

  def wordToData(source: ByteBuffer): Data = {
    wordToBytes(source)
  }

  val FALSE: Data = bytes(0)
  val TRUE: Data = bytes(1)

  def boolToData(b: Boolean): Data = {
    if(b) TRUE else FALSE
  }

  def dataToBool(d: Data): Boolean = {
    d.reverseIterator.exists(_ != 0.toByte)
  }

}
