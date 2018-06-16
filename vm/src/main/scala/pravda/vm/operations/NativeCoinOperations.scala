package pravda.vm.operations

import pravda.common.domain
import pravda.vm.state.VmError.OperationDenied
import pravda.vm.state.{Environment, Memory, VmErrorException}
import pravda.vm.watt.WattCounter
import pravda.vm.watt.WattCounter._

final class NativeCoinOperations(memory: Memory,
                                 environment: Environment,
                                 wattCounter: WattCounter,
                                 maybeProgramAddress: Option[domain.Address]) {

  def ptransfer(): Unit = maybeProgramAddress match {
    case None => throw VmErrorException(OperationDenied)
    case Some(programAddress) =>
      val amount = coin(memory.pop())
      val to = address(memory.pop())
      wattCounter.cpuUsage(CpuStorageUse)
      environment.transfer(programAddress, to, amount)
  }

  def transfer(): Unit = {
    val amount = coin(memory.pop())
    val to = address(memory.pop())
    wattCounter.cpuUsage(CpuStorageUse)
    environment.transfer(environment.executor, to, amount)
  }
}
