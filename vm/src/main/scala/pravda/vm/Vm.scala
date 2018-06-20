package pravda

package vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.domain
import pravda.common.domain.Address
import pravda.vm.operations._
import pravda.vm.state.VmError._
import pravda.vm.state._
import pravda.vm.watt.WattCounter
import pravda.vm.watt.WattCounter._

import scala.annotation.switch
import scala.collection.mutable.ArrayBuffer

trait Vm {

  def spawn(program: ByteBuffer,
            environment: Environment,
            memory: Memory,
            wattCounter: WattCounter,
            maybeStorage: Option[Storage],
            programAddress: Option[domain.Address],
            pcallAllowed: Boolean): ExecutionResult
}

object Vm extends Vm {

  import Opcodes.int._

  def spawn(program: ByteString, executor: Address, environment: Environment, wattLimit: Long): ExecutionResult =
    spawn(
      program = ByteBuffer.wrap(program.toByteArray),
      environment = environment,
      memory = VmMemory.empty,
      counter = new WattCounter(wattLimit),
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
      case None => ExecutionResult(memory, Some(VmErrorException(NoSuchProgram)), wattCounter)
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
    val logicalOperations = new LogicalOperations(memory, counter)
    val arithmeticOperations = new ArithmeticOperations(memory, counter)
    val storageOperations = new StorageOperations(memory, maybeStorage, counter)
    val heapOperations = new HeapOperations(memory, counter)
    val stackOperations = new StackOperations(memory, counter)
    val controlOperations = new ControlOperations(program, callStack, memory, counter)
    val nativeCoinOperations = new NativeCoinOperations(memory, environment, counter, maybeProgramAddress)
    val dataOperations = new DataOperations(memory, counter)
    val systemOperations = new SystemOperations(memory, maybeStorage, counter, environment, maybeProgramAddress, this)

    var lastOpcodePosition: Int = -1

    try {
      var continue = true
      while (continue && program.hasRemaining) {
        lastOpcodePosition = program.position()
        counter.cpuUsage(CpuBasic)
        (program.get() & 0xff: @switch) match {
          // Control operations
          case CALL  => controlOperations.call()
          case RET   => controlOperations.ret()
          case JUMP  => controlOperations.jump()
          case JUMPI => controlOperations.jumpi()
          // Native coin operations
          case TRANSFER  => nativeCoinOperations.transfer()
          case PTRANSFER => nativeCoinOperations.ptransfer()
          // Data? operations
          case SLICE  => dataOperations.slice()
          case CONCAT => dataOperations.concat()
          // Stack operations
          case PUSHX =>
            Data.readFromByteBuffer(program) match {
              case data: Data.Primitive =>
                counter.memoryUsage(data.volume.toLong)
                memory.push(data)
              case _ => throw VmErrorException(VmError.WrongType)
            }
          case POP   => memory.pop()
          case DUP   => stackOperations.dup()
          case DUPN  => stackOperations.dupN()
          case SWAP  => stackOperations.swap()
          case SWAPN => stackOperations.swapN()
          // Heap operations
          case MPUT => heapOperations.mput()
          case MGET => heapOperations.mget()
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
          // System operations
          case STOP    => continue = false
          case FROM    => systemOperations.from()
          case LCALL   => systemOperations.lcall()
          case PCREATE => systemOperations.pcreate()
          case PUPDATE => systemOperations.pupdate()
          case PADDR   => systemOperations.paddr()
          case PCALL =>
            if (pcallAllowed) {
              systemOperations.pcall()
            }
        }
      }
      ExecutionResult(memory, None, counter)
    } catch {
      case err: VmErrorException =>
        err.addToTrace(Point(callStack, lastOpcodePosition, maybeProgramAddress))
        ExecutionResult(memory, Some(err), counter)
      case cause: Throwable =>
        val err =
          VmErrorException(SomethingWrong(cause)).addToTrace(Point(callStack, lastOpcodePosition, maybeProgramAddress))
        ExecutionResult(memory, Some(VmErrorException(SomethingWrong(err))), counter)
    }
  }
}
