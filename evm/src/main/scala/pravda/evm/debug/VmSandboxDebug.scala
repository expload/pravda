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

import com.google.protobuf.ByteString
import pravda.common.domain.Address
import pravda.vm
import pravda.vm._
import pravda.vm.impl.{MemoryImpl, WattCounterImpl}
import pravda.vm.sandbox.VmSandbox._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object VmSandboxDebug {

  def run[S](input: Preconditions, code: ByteString)(implicit debugger: Debugger[S]): List[S] = {
    val sandboxVm = new VmImplDebug()
    val heapSandbox = heap(input)
    val memory = MemoryImpl(ArrayBuffer(input.stack: _*), heapSandbox)
    val wattCounter = new WattCounterImpl(input.`watts-limit`)

    val pExecutor = input.executor.getOrElse {
      Address @@ ByteString.copyFrom((1 to 32).map(_.toByte).toArray)
    }

    val effects = mutable.Buffer[vm.Effect]()
    val environmentS: Environment = environment(input, effects, pExecutor)
    val storage = new StorageSandbox(Address.Void, effects, input.storage.toSeq)

    memory.enterProgram(Address.Void)
    val res = sandboxVm.debugBytes(
      code.asReadOnlyByteBuffer(),
      environmentS,
      memory,
      wattCounter,
      storage,
      Some(Address.Void),
      pcallAllowed = true
    )
    memory.exitProgram()
    res

  }
}
