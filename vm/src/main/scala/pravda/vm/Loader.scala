package pravda.vm

import pravda.common.domain.Address
import state.Environment

trait Loader {

  def lib(address: Address, worldState: Environment): Option[Library]

}
