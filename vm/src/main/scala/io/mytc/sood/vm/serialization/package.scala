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

}
