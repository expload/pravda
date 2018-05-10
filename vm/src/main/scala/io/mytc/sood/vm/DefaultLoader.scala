package io.mytc.sood
package vm
import io.mytc.sood.vm.state.{Address, WorldState}

object DefaultLoader extends Loader {
  val stdLoader: Loader = std.Loader
  val udfLoader: Loader = udf.Loader

  override def lib(address: Address, worldState: WorldState): Option[Library] =
    stdLoader.lib(address, worldState).orElse(udfLoader.lib(address, worldState))

}
