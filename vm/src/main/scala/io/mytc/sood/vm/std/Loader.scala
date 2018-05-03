package io.mytc.sood.vm
package std

import java.nio.charset.StandardCharsets

import state.{WorldState, Address}

object Loader extends Loader {

  val libraries: Seq[Lib] = Array(
    libs.Math
  )

  private lazy val libsTable = libraries.map(l => l.address -> l).toMap

  override def lib(address: Address, worldState: WorldState): Option[Lib] =
    libsTable.get(new String(address.toArray, StandardCharsets.UTF_8))

}
