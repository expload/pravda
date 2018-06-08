package pravda.vm.state

import pravda.vm.watt.WattCounter

final case class ExecutionResult(
    memory: Memory,
    error: Option[VmErrorException],
    wattCounter: WattCounter
) {

  def isSuccess: Boolean = error.isEmpty

}
