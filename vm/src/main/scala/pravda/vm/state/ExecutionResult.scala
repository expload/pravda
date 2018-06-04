package pravda.vm.state

import pravda.vm.watt.WattCounter

final case class ExecutionResult(
    memory: VmMemory,
    error: Option[VmErrorException],
    wattCounter: WattCounter
)
