package pravda

package vm

import pravda.common.domain.Address
import pravda.vm.state.Environment

object DefaultLoader extends Loader {
  val stdLoader: Loader = std.Loader
  val udfLoader: Loader = udf.Loader

  override def lib(address: Address, worldState: Environment): Option[Library] =
    stdLoader.lib(address, worldState).orElse(udfLoader.lib(address, worldState))

}
