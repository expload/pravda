package io.mytc.sood.libs

import scodec.bits.ByteVector

import scala.collection.mutable.Map

case class ExternalTable() {
  private val table = Map.empty[ByteVector, Int]

  def put(name: Array[Byte], pos: Int): Unit = {
    table += (ByteVector(name) -> pos)
  }

  def get(name: Array[Byte]): Option[Int] = table.get(ByteVector(name))

}
