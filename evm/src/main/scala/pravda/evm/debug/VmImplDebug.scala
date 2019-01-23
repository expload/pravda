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
import pravda.vm.Error.PcallDenied
import pravda.vm.Opcodes._
import pravda.vm.WattCounter.CpuBasic
import pravda.vm._
import pravda.vm.impl.MemoryImpl
import pravda.vm.operations._

import scala.annotation.tailrec
import scala.util.Try
import pravda.evm.debug.DebugVm.{InterruptedExecution, MetaExecution, UnitExecution}
import pravda.vm.sandbox.VmSandbox.StorageSandbox

class VmImplDebug extends DebugVm {

  def debugBytes[S](program: ByteBuffer,
                    env: Environment,
                    mem: MemoryImpl,
                    counter: WattCounter,
                    maybeStorage: Option[StorageSandbox],
                    maybePA: Option[domain.Address],
                    pcallAllowed: Boolean)(implicit debugger: Debugger[S]): List[S] = {

    val logicalOperations = new LogicalOperations(mem, counter)
    val arithmeticOperations = new ArithmeticOperations(mem, counter)
    val storageOperations = new StorageOperations(mem, maybeStorage, counter)
    val heapOperations = new HeapOperations(mem, program, counter)
    val stackOperations = new StackOperations(mem, program, counter)
    val controlOperations = new ControlOperations(program, mem, counter)
    val nativeCoinOperations = new NativeCoinOperations(mem, env, counter, maybePA)
    val systemOperations =
      new SystemOperations(program, mem, maybeStorage, counter, env, maybePA, StandardLibrary.Index, this)
    val dataOperations = new DataOperations(mem, counter)

    val Some(storage) = maybeStorage

    @tailrec def proc(acc: List[S]): List[S] = {
      counter.cpuUsage(CpuBasic)
      val op = program.get() & 0xff

      import DebugVm.ExecutionResult
      val executionResult = Try[ExecutionResult] {
        mem.setCounter(program.position())
        op match {
          // Control operations
          case CALL  => UnitExecution(() => controlOperations.call())
          case RET   => UnitExecution(() => controlOperations.ret())
          case JUMP  => UnitExecution(() => controlOperations.jump())
          case JUMPI => UnitExecution(() => controlOperations.jumpi())
          // Native coin operations
          case TRANSFER  => UnitExecution(() => nativeCoinOperations.transfer())
          case PTRANSFER => UnitExecution(() => nativeCoinOperations.ptransfer())
          case BALANCE   => UnitExecution(() => nativeCoinOperations.balance())
          // Stack operations
          case POP   => UnitExecution(() => stackOperations.pop())
          case PUSHX => UnitExecution(() => stackOperations.push())
          case DUP   => UnitExecution(() => stackOperations.dup())
          case DUPN  => UnitExecution(() => stackOperations.dupN())
          case SWAP  => UnitExecution(() => stackOperations.swap())
          case SWAPN => UnitExecution(() => stackOperations.swapN())
          // Heap operations
          case NEW               => UnitExecution(() => heapOperations.`new`())
          case ARRAY_GET         => UnitExecution(() => heapOperations.arrayGet())
          case STRUCT_GET        => UnitExecution(() => heapOperations.structGet())
          case STRUCT_GET_STATIC => UnitExecution(() => heapOperations.structGetStatic())
          case ARRAY_MUT         => UnitExecution(() => heapOperations.arrayMut())
          case STRUCT_MUT        => UnitExecution(() => heapOperations.structMut())
          case STRUCT_MUT_STATIC => UnitExecution(() => heapOperations.structMutStatic())
          case PRIMITIVE_PUT     => UnitExecution(() => heapOperations.primitivePut())
          case PRIMITIVE_GET     => UnitExecution(() => heapOperations.primitiveGet())
          case NEW_ARRAY         => UnitExecution(() => heapOperations.newArray())
          case LENGTH            => UnitExecution(() => heapOperations.length())
          // Storage operations
          case SPUT   => UnitExecution(() => storageOperations.put())
          case SGET   => UnitExecution(() => storageOperations.get())
          case SDROP  => UnitExecution(() => storageOperations.drop())
          case SEXIST => UnitExecution(() => storageOperations.exists())
          // Arithmetic operations
          case ADD => UnitExecution(() => arithmeticOperations.add())
          case MUL => UnitExecution(() => arithmeticOperations.mul())
          case DIV => UnitExecution(() => arithmeticOperations.div())
          case MOD => UnitExecution(() => arithmeticOperations.mod())
          // Logical operations
          case NOT => UnitExecution(() => logicalOperations.not())
          case AND => UnitExecution(() => logicalOperations.and())
          case OR  => UnitExecution(() => logicalOperations.or())
          case XOR => UnitExecution(() => logicalOperations.xor())
          case EQ  => UnitExecution(() => logicalOperations.eq())
          case LT  => UnitExecution(() => logicalOperations.lt())
          case GT  => UnitExecution(() => logicalOperations.gt())
          // Data operations
          case CAST   => UnitExecution(() => dataOperations.cast())
          case CONCAT => UnitExecution(() => dataOperations.concat())
          case SLICE  => UnitExecution(() => dataOperations.slice())
          // System operations
          case STOP    => InterruptedExecution
          case FROM    => UnitExecution(() => systemOperations.from())
          case LCALL   => UnitExecution(() => systemOperations.lcall())
          case SCALL   => UnitExecution(() => systemOperations.scall())
          case PCREATE => UnitExecution(() => systemOperations.pcreate())
          case SEAL    => UnitExecution(() => systemOperations.seal())
          case PUPDATE => UnitExecution(() => systemOperations.pupdate())
          case PADDR   => UnitExecution(() => systemOperations.paddr())
          case PCALL =>
            if (pcallAllowed) {
              UnitExecution(() => systemOperations.pcall())
            } else {
              throw ThrowableVmError(PcallDenied)
            }
          case THROW   => UnitExecution(() => systemOperations.`throw`())
          case EVENT   => UnitExecution(() => systemOperations.event())
          case CALLERS => UnitExecution(() => systemOperations.callers())
          case HEIGHT  => UnitExecution(() => systemOperations.chainHeight())
          case HASH    => UnitExecution(() => systemOperations.lastBlockHash())
          case CODE    => UnitExecution(() => systemOperations.code())
          case META =>
            MetaExecution(Meta.readFromByteBuffer(program))
          case _ => UnitExecution(() => ())
        }
      }.toEither
      val state = debugger.debugOp(program, op, mem, storage)(executionResult)
      val res = state :: acc
      executionResult match {
        case Right(InterruptedExecution) | Left(_) =>
          res
        case _ => if (program.hasRemaining) proc(res) else res
      }
    }
    proc(Nil).reverse
  }
}
