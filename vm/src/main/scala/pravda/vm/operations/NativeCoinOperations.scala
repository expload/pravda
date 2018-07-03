package pravda.vm.operations

import pravda.common.domain
import pravda.vm.VmError.OperationDenied
import pravda.vm.WattCounter._
import pravda.vm.operations.annotation.OpcodeImplementation
import pravda.vm.{Environment, Memory, VmErrorException, WattCounter}
import pravda.vm.Opcodes._
final class NativeCoinOperations(memory: Memory,
                                 environment: Environment,
                                 wattCounter: WattCounter,
                                 maybeProgramAddress: Option[domain.Address]) {

  @OpcodeImplementation(
    opcode = PTRANSFER,
    description = "Gets two parameters `a` and `n` from " +
      "the stack and transfers `n` native coins from " +
      "the current program account to the account `a`"
  )
  def ptransfer(): Unit = maybeProgramAddress match {
    case None => throw VmErrorException(OperationDenied)
    case Some(programAddress) =>
      val amount = coins(memory.pop())
      val to = address(memory.pop())
      wattCounter.cpuUsage(CpuStorageUse)
      environment.transfer(programAddress, to, amount)
  }

  @OpcodeImplementation(
    opcode = TRANSFER,
    description = "Gets two parameters `a` and `n` from " +
      "the stack and transfers `n` native coins " +
      "from the executor account to the account `a`."
  )
  def transfer(): Unit = {
    val amount = coins(memory.pop())
    val to = address(memory.pop())
    wattCounter.cpuUsage(CpuStorageUse)
    environment.transfer(environment.executor, to, amount)
  }
}
