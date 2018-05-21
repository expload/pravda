package pravda.vm

import state.{Address, Environment}

trait Loader {

  def lib(address: Address, worldState: Environment): Option[Library]

}
