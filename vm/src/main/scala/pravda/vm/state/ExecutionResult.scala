package pravda.vm.state

import pravda.vm.watt.WattCounter

case class ExecutionResult(
                            memory: Memory,
                            error: Option[VmErrorException],
                            wattCounter: WattCounter
                          )
