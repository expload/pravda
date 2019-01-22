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

import cats.Applicative
import cats.kernel.Monoid
import com.google.protobuf.ByteString
import pravda.common.domain
import pravda.common.domain.Address
import pravda.vm._
import pravda.vm.impl.MemoryImpl
import pravda.vm.sandbox.VmSandbox.StorageSandbox

import scala.language.higherKinds

//'extends Vm' is required for using 'this' in SystemOperation constructor
trait DebugVm extends Vm {
  override def spawn(initialProgram: ByteString, environment: Environment, wattLimit: Long): ExecutionResult =
    throw new Exception("It's debug vm")

  override def run(programAddress: Address,
                   environment: Environment,
                   memory: Memory,
                   wattCounter: WattCounter,
                   pcallAllowed: Boolean): Unit =
    throw new Exception("It's debug vm. You can't use pcall, lcall opcodes")

  def debugBytes[F[_], S](
      program: ByteBuffer,
      env: Environment,
      mem: MemoryImpl,
      counter: WattCounter,
      maybeStorage: Option[StorageSandbox],
      maybePA: Option[domain.Address],
      pcallAllowed: Boolean)(implicit monoid: Monoid[F[S]], appl: Applicative[F], debugger: Debugger[S]): F[S]
}

object DebugVm {

  sealed trait ExecutionResult

  final case class UnitExecution(f: () => Unit) extends ExecutionResult {
    f()
  }

  case object InterruptedExecution extends ExecutionResult

  final case class MetaExecution(meta: Meta) extends ExecutionResult

}
