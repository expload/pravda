package io.mytc.sood
package std

import java.nio.charset.StandardCharsets

object Libs extends Loader {

  val libraries: Seq[Lib] = Array(
    libs.Math
  )

  private lazy val libsTable = libraries.map(l => l.address -> l).toMap
  def lib(address: String): Option[Lib] = libsTable.get(address)

  override def lib(address: Array[Byte]): Option[Lib] = libsTable.get(new String(address, StandardCharsets.UTF_8))

}
