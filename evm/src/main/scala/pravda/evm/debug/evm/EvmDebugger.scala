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

package pravda.evm.debug.evm

import java.nio.ByteBuffer

import pravda.evm.debug.Debugger
import pravda.vm.Meta._
import pravda.vm.Opcodes.META
import pravda.vm.impl.MemoryImpl
import pravda.vm._
import pravda.vm.asm.Operation.mnemonicByOpcode

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

sealed trait DebugLog

final case class EvmOpLog(op: String)                              extends DebugLog
final case class PravdaOpLog(op: String, snapshot: MemorySnapshot) extends DebugLog
final case class ErrorLog(op: String)                              extends DebugLog

case class MemorySnapshot(stack: List[Data.Primitive], heap: List[Data])

object EvmDebugger extends Debugger[ArrayBuffer[DebugLog]] {
  override def debugOp(state: ArrayBuffer[DebugLog], program: ByteBuffer, op: Int, mem: MemoryImpl)(
      exec: () => Try[Unit]): (ArrayBuffer[DebugLog], Boolean) = {
    var cont = true
    op match {
      case META =>
        val meta = Meta.readFromByteBuffer(program)
        val snapshot = MemorySnapshot(mem.stack.toList, mem.heap.toList)
        meta match {
          case Custom(s) if s.startsWith(EvmDebugTranslator.debugMarker) =>
            state.append(EvmOpLog(s.stripPrefix(EvmDebugTranslator.debugMarker)))
          case l => state.append(PravdaOpLog(l.toString, snapshot))
        }

      case _ =>
        val t = exec()
        val snapshot = MemorySnapshot(mem.stack.toList, mem.heap.toList)
        state.append(PravdaOpLog(mnemonicByOpcode(op), snapshot))
        t.failed.foreach {
          case e: Data.DataException =>
            state.append(ErrorLog(e.toString))
            cont = false
          case ThrowableVmError(e) =>
            cont = false
            state.append(ErrorLog(e.toString))
        }
    }
    state -> cont
  }
  override def initial: ArrayBuffer[DebugLog] = ArrayBuffer.empty[DebugLog]
}
