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

import cats.Applicative
import cats.kernel.Monoid
import com.google.protobuf.ByteString
import pravda.common.domain.Address
import pravda.vm
import pravda.vm._
import pravda.vm.impl.{MemoryImpl, WattCounterImpl}
import pravda.vm.sandbox.VmSandbox.{EnvironmentSandbox, Preconditions, StorageSandbox}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.higherKinds

object VmSandboxDebug {

  def run[F[_], S](input: Preconditions, code: ByteString)(implicit debugger: Debugger[S],
                                                           monoid: Monoid[F[S]],
                                                           appl: Applicative[F]): F[S] = {
    val sandboxVm = new VmImplDebug()
    val heap = {
      if (input.heap.nonEmpty) {
        val length = input.heap.map(_._1.data).max + 1
        val buffer = ArrayBuffer.fill[Data](length)(Data.Primitive.Null)
        input.heap.foreach { case (ref, value) => buffer(ref.data) = value }
        buffer
      } else {
        ArrayBuffer[Data]()
      }
    }
    val memory = MemoryImpl(ArrayBuffer(input.stack: _*), heap)
    val wattCounter = new WattCounterImpl(input.`watts-limit`)

    val pExecutor = input.executor.getOrElse {
      Address @@ ByteString.copyFrom((1 to 32).map(_.toByte).toArray)
    }

    val effects = mutable.Buffer[vm.Effect]()
    val environment: Environment = new EnvironmentSandbox(
      effects,
      input.`program-storage`,
      input.balances.toSeq,
      input.programs.toSeq,
      pExecutor,
      input.`app-state-info`
    )
    val storage = new StorageSandbox(Address.Void, effects, input.storage.toSeq)

    memory.enterProgram(Address.Void)
    val res = sandboxVm.debugBytes(
      code.asReadOnlyByteBuffer(),
      environment,
      memory,
      wattCounter,
      Some(storage),
      Some(Address.Void),
      pcallAllowed = true
    )
    memory.exitProgram()
    res

  }
}
