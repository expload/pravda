package io.mytc.sood.vm

import state.{Address, WorldState}

trait Loader {

  def lib(address: Address, worldState: WorldState): Option[Library]

}
