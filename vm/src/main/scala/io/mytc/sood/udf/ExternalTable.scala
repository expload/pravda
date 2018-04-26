package io.mytc.sood.udf

import scodec.bits.ByteVector

import scala.collection.mutable

case class ExternalTable() {
  private val table = mutable.Map.empty[ByteVector, Int]

  def put(name: Array[Byte], pos: Int): Unit = {
    table += (ByteVector(name) -> pos)
  }

  def get(name: Array[Byte]): Option[Int] = table.get(ByteVector(name))

}
