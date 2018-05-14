package io.mytc.sood.vm
package std

import java.nio.charset.StandardCharsets

import scodec.bits.ByteVector

trait Lib extends Library {

  val address: String
  val functions: Seq[Func]

  private lazy val functionTable: Map[String, Func] = functions.map(f => f.name -> f).toMap

  def func(name: String): Option[Func] = functionTable.get(name)
  override def func(name: ByteVector): Option[Func] =
    functionTable.get(new String(name.toArray, StandardCharsets.UTF_8))

}
