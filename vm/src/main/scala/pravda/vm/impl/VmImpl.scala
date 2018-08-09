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
import pravda.vm.Meta.{GlobalMeta, SegmentMeta}
import pravda.vm.VmError.{NoSuchProgram, UnexpectedError}
import pravda.vm.WattCounter.{CpuBasic, CpuStorageUse}
import pravda.vm._
import pravda.vm.operations._

import scala.annotation.switch
import scala.collection.mutable.ArrayBuffer

class VmImpl extends Vm {

  import Opcodes._

  def spawn(program: ByteString,
            environment: Environment,
            memory: Memory,
            wattCounter: WattCounter,
            executor: Address): ExecutionResult =
    spawn(
      program = ByteBuffer.wrap(program.toByteArray),
      environment = environment,
      memory = memory,
      counter = wattCounter,
      maybeStorage = None,
      maybeProgramAddress = None,
      pcallAllowed = true
    )

  def spawn(programAddress: domain.Address,
            environment: Environment,
            memory: Memory,
            wattCounter: WattCounter,
            pcallAllowed: Boolean): ExecutionResult = {
    wattCounter.cpuUsage(CpuStorageUse)
    environment.getProgram(programAddress) match {
      case None => ExecutionResult(memory, Some(VmErrorResult(NoSuchProgram, Seq.empty, Seq.empty, None)), wattCounter)
      case Some(program) =>
        program.code.rewind()
        spawn(program.code, environment, memory, wattCounter, Some(program.storage), Some(programAddress), pcallAllowed)
    }
  }

  def spawn(program: ByteBuffer,
            environment: Environment,
            memory: Memory,
            counter: WattCounter,
            maybeStorage: Option[Storage],
            maybeProgramAddress: Option[domain.Address],
            pcallAllowed: Boolean): ExecutionResult = {

    val callStack = new ArrayBuffer[Int](1024)
    val callMetaStack = new ArrayBuffer[List[Meta]](1024)
    val curMetas = new ArrayBuffer[Meta](1024)

    def filterMetas(pred: Meta => Boolean) = {
      val newMetas = curMetas.filter(pred)
      curMetas.clear()
      curMetas ++= newMetas
    }

    val logicalOperations = new LogicalOperations(memory, counter)
    val arithmeticOperations = new ArithmeticOperations(memory, counter)
    val storageOperations = new StorageOperations(memory, maybeStorage, counter)
    val heapOperations = new HeapOperations(memory, program, counter)
    val stackOperations = new StackOperations(memory, program, counter)
    val controlOperations = new ControlOperations(program, callStack, curMetas, callMetaStack, memory, counter)
    val nativeCoinOperations = new NativeCoinOperations(memory, environment, counter, maybeProgramAddress)
    val systemOperations =
      new SystemOperations(memory, maybeStorage, counter, environment, maybeProgramAddress, StandardLibrary.Index, this)
    val dataOperations = new DataOperations(memory, counter)

    var lastOpcodePosition: Int = -1

    try {
      var continue = true
      while (continue && program.hasRemaining) {
        lastOpcodePosition = program.position()
        counter.cpuUsage(CpuBasic)
        val op = program.get() & 0xff
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
          case OWNER   => systemOperations.owner()
          case LCALL   => systemOperations.lcall()
          case SCALL   => systemOperations.scall()
          case PCREATE => systemOperations.pcreate()
          case SEAL    => systemOperations.seal()
          case PUPDATE => systemOperations.pupdate()
          case PADDR   => systemOperations.paddr()
          case PCALL =>
            if (pcallAllowed) {
              systemOperations.pcall()
            }
          case _ =>
        }

        op match {
          case META =>
            val newMeta = Meta.readFromByteBuffer(program)
            if (newMeta.isInstanceOf[Meta.SegmentMeta]) {
              filterMetas(_.getClass != newMeta.getClass)
            }
            curMetas += newMeta
          case _ =>
            filterMetas(m => m.isInstanceOf[GlobalMeta] || m.isInstanceOf[SegmentMeta])
        }
      }
      ExecutionResult(memory, None, counter)
    } catch {
      case err: Throwable =>
        callStack += lastOpcodePosition
        callMetaStack += curMetas.toList

        err match {
          case VmErrorException(e) =>
            ExecutionResult(
              memory,
              Some(VmErrorResult(e, callStack :+ lastOpcodePosition, callMetaStack, maybeProgramAddress)),
              counter
            )
          case cause: Throwable =>
            ExecutionResult(
              memory,
              Some(VmErrorResult(UnexpectedError(cause), callStack, callMetaStack, maybeProgramAddress)),
              counter
            )
        }
    }
  }
}
