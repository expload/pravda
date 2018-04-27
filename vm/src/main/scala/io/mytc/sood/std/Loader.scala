package io.mytc.sood
package std

import java.nio.charset.StandardCharsets

import vm.state.WorldState

object Loader extends Loader {

  val libraries: Seq[Lib] = Array(
    libs.Math
  )

  private lazy val libsTable = libraries.map(l => l.address -> l).toMap

  override def lib(address: Array[Byte], worldState: WorldState): Option[Lib] =
    libsTable.get(new String(address, StandardCharsets.UTF_8))

}
