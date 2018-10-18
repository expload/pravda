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

package pravda.vm.impl

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.domain
import pravda.common.domain.Address
import pravda.vm.Error.{DataError, NoSuchProgram, PcallDenied}
import pravda.vm.WattCounter.{CpuBasic, CpuStorageUse}
import pravda.vm._
import pravda.vm.operations._

import scala.annotation.switch

class VmImpl extends Vm {

  import Opcodes._

  private def makeFinalState(memory: Memory, wattCounter: WattCounter) =
    FinalState(wattCounter.spent, wattCounter.refund, wattCounter.total, memory.stack, memory.heap)

  /**
    * New vm "from scratch". Clear memory.
    * Initial program has no address.
    * Storage operations is not allowed.
    * PCall/LCall allowed.
    */
  def spawn(initialProgram: ByteString, environment: Environment, wattLimit: Long): ExecutionResult = {
    val mem = MemoryImpl.empty
    val counter = new WattCounterImpl(wattLimit)
    try {
      runBytes(
        program = initialProgram.asReadOnlyByteBuffer(),
        env = environment,
        mem = mem,
        counter = counter,
        maybeStorage = None,
        maybePA = None,
        pcallAllowed = true
      )
      Right(makeFinalState(mem, counter))
    } catch {
      case e: Data.DataException =>
        Left(RuntimeException(DataError(e.getMessage), makeFinalState(mem, counter), mem.callStack, mem.currentOffset))
      case ThrowableVmError(e) =>
        Left(RuntimeException(e, makeFinalState(mem, counter), mem.callStack, mem.currentOffset))
    }
  }

  /**
    * Run a program inside spawned VM.
    */
  override def run(programAddress: Address,
                   environment: Environment,
                   memory: Memory,
                   wattCounter: WattCounter,
                   pcallAllowed: Boolean): Unit = {
    wattCounter.cpuUsage(CpuStorageUse)
    environment.getProgram(programAddress) match {
      case Some(program) =>
        runBytes(program.code.asReadOnlyByteBuffer(),
                 environment,
                 memory,
                 wattCounter,
                 Some(program.storage),
                 Some(programAddress),
                 pcallAllowed)
      case None =>
        throw ThrowableVmError(NoSuchProgram)
    }
  }

  def runBytes(program: ByteBuffer,
               env: Environment,
               mem: Memory,
               counter: WattCounter,
               maybeStorage: Option[Storage],
               maybePA: Option[domain.Address],
               pcallAllowed: Boolean): Unit = {

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

    var continue = true
    while (continue && program.hasRemaining) {
      counter.cpuUsage(CpuBasic)
      val op = program.get() & 0xff
      mem.updateOffset(program.position())
      (op: @switch) match {
        // Control operations
        case CALL  => controlOperations.call()
        case RET   => controlOperations.ret()
        case JUMP  => controlOperations.jump()
        case JUMPI => controlOperations.jumpi()
        // Native coin operations
        case TRANSFER  => nativeCoinOperations.transfer()
        case PTRANSFER => nativeCoinOperations.ptransfer()
        case BALANCE   => nativeCoinOperations.balance()
        // Stack operations
        case POP   => stackOperations.pop()
        case PUSHX => stackOperations.push()
        case DUP   => stackOperations.dup()
        case DUPN  => stackOperations.dupN()
        case SWAP  => stackOperations.swap()
        case SWAPN => stackOperations.swapN()
        // Heap operations
        case NEW               => heapOperations.`new`()
        case ARRAY_GET         => heapOperations.arrayGet()
        case STRUCT_GET        => heapOperations.structGet()
        case STRUCT_GET_STATIC => heapOperations.structGetStatic()
        case ARRAY_MUT         => heapOperations.arrayMut()
        case STRUCT_MUT        => heapOperations.structMut()
        case STRUCT_MUT_STATIC => heapOperations.structMutStatic()
        case PRIMITIVE_PUT     => heapOperations.primitivePut()
        case PRIMITIVE_GET     => heapOperations.primitiveGet()
        case NEW_ARRAY         => heapOperations.newArray()
        case LENGTH            => heapOperations.length()
        // Storage operations
        case SPUT   => storageOperations.put()
        case SGET   => storageOperations.get()
        case SDROP  => storageOperations.drop()
        case SEXIST => storageOperations.exists()
        // Arithmetic operations
        case ADD => arithmeticOperations.add()
        case MUL => arithmeticOperations.mul()
        case DIV => arithmeticOperations.div()
        case MOD => arithmeticOperations.mod()
        // Logical operations
        case NOT => logicalOperations.not()
        case AND => logicalOperations.and()
        case OR  => logicalOperations.or()
        case XOR => logicalOperations.xor()
        case EQ  => logicalOperations.eq()
        case LT  => logicalOperations.lt()
        case GT  => logicalOperations.gt()
        // Data operations
        case CAST   => dataOperations.cast()
        case CONCAT => dataOperations.concat()
        case SLICE  => dataOperations.slice()
        // System operations
        case STOP    => continue = false
        case FROM    => systemOperations.from()
        case LCALL   => systemOperations.lcall()
        case SCALL   => systemOperations.scall()
        case PCREATE => systemOperations.pcreate()
        case SEAL    => systemOperations.seal()
        case PUPDATE => systemOperations.pupdate()
        case PADDR   => systemOperations.paddr()
        case PCALL =>
          if (pcallAllowed) {
            systemOperations.pcall()
          } else {
            throw ThrowableVmError(PcallDenied)
          }
        case THROW => systemOperations.`throw`()
        case EVENT => systemOperations.event()
        case META  => Meta.readFromByteBuffer(program)
        case _     =>
      }
    }
  }
}
