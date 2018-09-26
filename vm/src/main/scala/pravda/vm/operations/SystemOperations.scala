/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.vm.operations

import pravda.common.domain
import pravda.common.domain.Address
import pravda.vm.Opcodes._
import pravda.vm.VmError.{OperationDenied, UserError}
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
          .error
          .foreach(e => throw VmErrorException(e.error))
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
          .error
          .foreach(e => throw VmErrorException(e.error))
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
      case None           => throw VmErrorException(VmError.InvalidAddress)
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
    if (environment.getProgramOwner(programAddress).contains(environment.executor)) {
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
    opcode = SEAL,
    description = "Takes the address of an existing program, makes the program sealed"
  )
  def seal(): Unit = {
    val programAddress = address(memory.pop())
    if (environment.getProgramOwner(programAddress).contains(environment.executor)) {
      wattCounter.cpuUsage(CpuStorageUse)
      environment.sealProgram(programAddress)
    } else {
      throw VmErrorException(OperationDenied)
    }
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

  @OpcodeImplementation(
    opcode = OWNER,
    description = "Gives program owner's address. " +
      "If there's no owner of the given address then the void address (32 zero bytes) is returned. "
  )
  def owner(): Unit = {
    val programAddress = address(memory.pop())
    val datum = address(environment.getProgramOwner(programAddress).getOrElse(Address.Void))
    wattCounter.memoryUsage(datum.volume.toLong)
    memory.push(datum)
  }

  @OpcodeImplementation(
    opcode = THROW,
    description = "Takes string from stack and throws an error with description " +
      "as given string that stops the program."
  )
  def `throw`(): Unit = {
    val message = utf8(memory.pop())
    throw VmErrorException(UserError(message))
  }

  @OpcodeImplementation(
    opcode = EVENT,
    description =
      "Takes string and arbitrary data from stack, create new event with name as given string and with given data.")
  def event(): Unit = {
    val name = utf8(memory.pop())
    val data = memory.pop()
    maybeProgramAddress match {
      case Some(addr) => environment.event(addr, name, data)
      case None       => throw VmErrorException(OperationDenied)
    }
  }
}
