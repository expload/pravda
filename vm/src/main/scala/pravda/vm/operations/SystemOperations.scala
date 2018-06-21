package pravda.vm.operations

import pravda.common.domain
import pravda.vm.VmError.OperationDenied
import pravda.vm.WattCounter._
import pravda.vm._

final class SystemOperations(memory: Memory,
                             currentStorage: Option[Storage],
                             wattCounter: WattCounter,
                             environment: Environment,
                             maybeProgramAddress: Option[domain.Address],
                             vm: Vm) {

  /**
    * Takes first item in stack as program address
    * and second item in stack as arguments count.
    * Spawns program instance with it's own storage
    * and current memory and environment.
    */
  def pcall(): Unit = {
    wattCounter.cpuUsage(CpuExtCall, CpuStorageUse)
    val argumentsCount = int32(memory.pop())
    val programAddress = address(memory.pop())
    environment.getProgram(programAddress) match {
      case None => throw VmErrorException(VmError.InvalidAddress)
      case Some(ProgramContext(storage, code)) =>
        memory.limit(argumentsCount)
        vm.spawn(code, environment, memory, wattCounter, Some(storage), Some(programAddress), pcallAllowed = true)
        memory.dropLimit()
    }
  }

  /**
    * Takes first item in stack as program address
    * and second item in stack as arguments count.
    * Spawns program instance with current storage, memory
    * and environment.
    */
  def lcall(): Unit = {
    val argumentsCount = int32(memory.pop())
    memory.pop() match {
      case Data.Primitive.Null =>
      // TODO use standard library
      case rawAddress =>
        wattCounter.cpuUsage(CpuStorageUse)
        environment.getProgram(address(rawAddress)) match {
          case None => throw VmErrorException(VmError.InvalidAddress)
          case Some(ProgramContext(_, code)) =>
            memory.limit(argumentsCount)
            vm.spawn(code, environment, memory, wattCounter, currentStorage, maybeProgramAddress, pcallAllowed = false)
            memory.dropLimit()
        }
    }
  }

  /**
    *
    */
  def pupdate(): Unit = {
    val programAddress = address(memory.pop())
    val code = memory.pop()
    wattCounter.cpuUsage(CpuStorageUse)
    if (environment.getProgramOwner(programAddress) == environment.executor) {
      val oldProgram = environment.getProgram(programAddress)
      val oldProgramSize = oldProgram.fold(0L)(_.code.remaining.toLong)
      wattCounter.cpuUsage(CpuStorageUse)
      wattCounter.storageUsage(occupiedBytes = code.volume.toLong, oldProgramSize)
      environment.updateProgram(programAddress, code)
    } else {
      throw VmErrorException(OperationDenied)
    }
  }

  /**
    */
  def pcreate(): Unit = {
    val code = memory.pop()
    val programAddress = environment.createProgram(environment.executor, code)
    val data = address(programAddress)
    wattCounter.cpuUsage(CpuStorageUse)
    wattCounter.storageUsage(occupiedBytes = code.volume.toLong)
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(address(programAddress))
  }

  /**
    *
    */
  def paddr(): Unit = {
    maybeProgramAddress match {
      case Some(programAddress) =>
        val data = address(programAddress)
        wattCounter.memoryUsage(data.volume.toLong)
        memory.push(data)
      case None => throw VmErrorException(OperationDenied)
    }
  }

  /**
    *
    */
  def from(): Unit = {
    val datum = address(environment.executor)
    wattCounter.memoryUsage(datum.volume.toLong)
    memory.push(datum)
  }
}
