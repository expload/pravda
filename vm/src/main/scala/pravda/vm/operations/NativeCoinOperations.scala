package pravda.vm.operations

import pravda.common.domain
import pravda.vm.VmError.OperationDenied
import pravda.vm.WattCounter._
import pravda.vm.{Environment, Memory, VmErrorException, WattCounter}

final class NativeCoinOperations(memory: Memory,
                                 environment: Environment,
                                 wattCounter: WattCounter,
                                 maybeProgramAddress: Option[domain.Address]) {

  def ptransfer(): Unit = maybeProgramAddress match {
    case None => throw VmErrorException(OperationDenied)
    case Some(programAddress) =>
      val amount = coins(memory.pop())
      val to = address(memory.pop())
      wattCounter.cpuUsage(CpuStorageUse)
      environment.transfer(programAddress, to, amount)
  }

  def transfer(): Unit = {
    val amount = coins(memory.pop())
    val to = address(memory.pop())
    wattCounter.cpuUsage(CpuStorageUse)
    environment.transfer(environment.executor, to, amount)
  }
}
