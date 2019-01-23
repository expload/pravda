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

import pravda.evm.debug.DebugVm.ExecutionResult
import pravda.vm.impl.MemoryImpl
import pravda.vm.sandbox.VmSandbox.StorageSandbox

trait Debugger[State] {

  def debugOp(program: ByteBuffer, op: Int, mem: MemoryImpl, storage: StorageSandbox)(
      execResult: Either[Throwable, ExecutionResult]): State
}
