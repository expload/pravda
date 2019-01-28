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

package pravda.evm.debug

import java.nio.ByteBuffer

import pravda.common.domain
import pravda.vm._
import pravda.vm.impl.MemoryImpl
import pravda.vm.sandbox.VmSandbox.StorageSandbox

trait DebugVm {

  def vm: Vm

  def debugBytes[S](program: ByteBuffer,
                    env: Environment,
                    mem: MemoryImpl,
                    counter: WattCounter,
                    storage: StorageSandbox,
                    maybePA: Option[domain.Address],
                    pcallAllowed: Boolean)(implicit debugger: Debugger[S]): List[S]
}

object DebugVm {

  sealed trait ExecutionResult

  final case class UnitExecution(f: () => Unit) extends ExecutionResult {
    f()
  }

  case object InterruptedExecution extends ExecutionResult

  final case class MetaExecution(meta: Meta) extends ExecutionResult

}
