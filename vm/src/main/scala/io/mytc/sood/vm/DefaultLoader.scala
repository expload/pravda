package io.mytc.sood
package vm
import io.mytc.sood.vm.state.WorldState

object DefaultLoader extends Loader {
  val stdLoader: Loader = std.Loader
  val udfLoader: Loader = udf.Loader

  override def lib(address: Array[Byte], worldState: WorldState): Option[Library] =
    stdLoader.lib(address, worldState).orElse(udfLoader.lib(address, worldState))

}
