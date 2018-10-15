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
import pravda.vm.Error.OperationDenied
import pravda.vm.WattCounter._
import pravda.vm.operations.annotation.OpcodeImplementation
import pravda.vm.{Environment, Memory, ThrowableVmError, WattCounter}
import pravda.vm.Opcodes._
final class NativeCoinOperations(memory: Memory,
                                 environment: Environment,
                                 wattCounter: WattCounter,
                                 maybeProgramAddress: Option[domain.Address]) {

  @OpcodeImplementation(
    opcode = BALANCE,
    description = "Takes address from stack, pushes native coin balance to the stack"
  )
  def balance(): Unit = {
    val addr = address(memory.pop())
    val balance = coins(environment.balance(addr))
    wattCounter.cpuUsage(CpuStorageUse)
    memory.push(balance)
  }

  @OpcodeImplementation(
    opcode = PTRANSFER,
    description = "Gets two parameters `a` and `n` from " +
      "the stack and transfers `n` native coins from " +
      "the current program account to the account `a`"
  )
  def ptransfer(): Unit = maybeProgramAddress match {
    case None => throw ThrowableVmError(OperationDenied)
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
