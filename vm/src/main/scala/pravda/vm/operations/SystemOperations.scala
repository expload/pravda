package pravda.vm.operations

import pravda.common.domain
import pravda.vm.Opcodes._
import pravda.vm.VmError.OperationDenied
import pravda.vm.WattCounter._
import pravda.vm._
import pravda.vm.operations.annotation.OpcodeImplementation

final class SystemOperations(memory: Memory,
                             currentStorage: Option[Storage],
                             wattCounter: WattCounter,
                             environment: Environment,
                             maybeProgramAddress: Option[domain.Address],
                             standardLibrary: Map[Long, (Memory, WattCounter) => Unit],
                             vm: Vm) {

  @OpcodeImplementation(
    opcode = STOP,
    description = "Stops program execution."
  )
  def stop(): Unit = {
    // See VmImpl!
  }

  @OpcodeImplementation(
    opcode = PCALL,
    description = "Takes two words by which it is followed. " +
      "They are address `a` and the number of parameters `n`, " +
      "respectively. Then it executes the smart contract with " +
      "the address `a` and passes there only $n$ top elements " +
      "of the stack."
  )
  def pcall(): Unit = {
    wattCounter.cpuUsage(CpuExtCall, CpuStorageUse)
    val argumentsCount = integer(memory.pop())
    val programAddress = address(memory.pop())
    environment.getProgram(programAddress) match {
      case None => throw VmErrorException(VmError.InvalidAddress)
      case Some(ProgramContext(storage, code)) =>
        memory.limit(argumentsCount.toInt)
        vm.spawn(code, environment, memory, wattCounter, Some(storage), Some(programAddress), pcallAllowed = true)
        memory.dropLimit()
    }
  }

  @OpcodeImplementation(
    opcode = LCALL,
    description = "Takes three words by which it is followed." +
      "They are address `a`, function `f` and the number of " +
      "parameters `n`, respectively. Then it executes the " +
      "function `f` of the library (which is a special form of program) " +
      "with the address `a` and passes there only $n$ top elements " +
      "of the stack."
  )
  def lcall(): Unit = {
    val argumentsCount = integer(memory.pop())
    val addr = address(memory.pop())
    environment.getProgram(addr) match {
      case None => throw VmErrorException(VmError.InvalidAddress)
      case Some(ProgramContext(_, code)) =>
        wattCounter.cpuUsage(CpuStorageUse)
        memory.limit(argumentsCount.toInt)
        vm.spawn(code, environment, memory, wattCounter, currentStorage, maybeProgramAddress, pcallAllowed = false)
        memory.dropLimit()
    }
  }

  @OpcodeImplementation(
    opcode = SCALL,
    description = "Takes id of function from standard library and execute it."
  )
  def scall(): Unit = {
    val id = integer(memory.pop())
    wattCounter.cpuUsage(CpuStorageUse)
    standardLibrary.get(id) match {
      case None => throw VmErrorException(VmError.InvalidAddress)
      case Some(function) => function(memory, wattCounter)
    }
  }

  @OpcodeImplementation(
    opcode = PUPDATE,
    description = "Takes address of a program and new bytecode. " +
      "Replaces bytecode in storage. This opcode can be performed " +
      "only from owner of the program"
  )
  def pupdate(): Unit = {
    val programAddress = address(memory.pop())
    val code = bytes(memory.pop())
    wattCounter.cpuUsage(CpuStorageUse)
    if (environment.getProgramOwner(programAddress) == environment.executor) {
      val oldProgram = environment.getProgram(programAddress)
      val oldProgramSize = oldProgram.fold(0L)(_.code.remaining.toLong)
      wattCounter.cpuUsage(CpuStorageUse)
      wattCounter.storageUsage(occupiedBytes = code.size().toLong, oldProgramSize)
      environment.updateProgram(programAddress, code)
    } else {
      throw VmErrorException(OperationDenied)
    }
  }

  @OpcodeImplementation(
    opcode = PCREATE,
    description = "Takes bytecode of a new program, put's it to " +
      "state and returns program address."
  )
  def pcreate(): Unit = {
    val code = bytes(memory.pop())
    val programAddress = environment.createProgram(environment.executor, code)
    val data = address(programAddress)
    wattCounter.cpuUsage(CpuStorageUse)
    wattCounter.storageUsage(occupiedBytes = code.size().toLong)
    wattCounter.memoryUsage(data.volume.toLong)
    memory.push(address(programAddress))
  }

  @OpcodeImplementation(
    opcode = PADDR,
    description = "Gives current program address."
  )
  def paddr(): Unit = {
    maybeProgramAddress match {
      case Some(programAddress) =>
        val data = address(programAddress)
        wattCounter.memoryUsage(data.volume.toLong)
        memory.push(data)
      case None => throw VmErrorException(OperationDenied)
    }
  }

  @OpcodeImplementation(
    opcode = FROM,
    description = "Gives current executor address."
  )
  def from(): Unit = {
    val datum = address(environment.executor)
    wattCounter.memoryUsage(datum.volume.toLong)
    memory.push(datum)
  }
}
