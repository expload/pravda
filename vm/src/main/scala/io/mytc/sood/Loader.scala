package io.mytc.sood

import io.mytc.sood.vm.state.WorldState

trait Loader {

  def lib(address: Array[Byte], worldState: WorldState): Option[Library]

}
