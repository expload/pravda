package io.mytc.sood
package std

import java.nio.charset.StandardCharsets

trait Lib extends Library {

  val address: String
  val functions: Seq[Func]

  private lazy val functionTable: Map[String, Func] = functions.map(f => f.name -> f).toMap

  def func(name: String): Option[Func] = functionTable.get(name)
  override def func(name: Array[Byte]): Option[Func] = functionTable.get(new String(name, StandardCharsets.UTF_8))

}
