package pravda.vm.state

case class ExecutionResult(
                            memory: Memory,
                            error: Option[VmError],
                            wattRemaining: Long,
                            resourcesInfo: ResourcesUsageInfo
                          )
