package pravda.vm.impl

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.domain
import pravda.common.domain.Address
import pravda.vm.StackTrace.Point
import pravda.vm.VmError.{NoSuchProgram, SomethingWrong}
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
    val heapOperations = new HeapOperations(memory, program, counter)
    val stackOperations = new StackOperations(memory, program, counter)
    val controlOperations = new ControlOperations(program, callStack, memory, counter)
    val nativeCoinOperations = new NativeCoinOperations(memory, environment, counter, maybeProgramAddress)
    val systemOperations = new SystemOperations(memory, maybeStorage, counter, environment, maybeProgramAddress, this)
    val dataOperations = new DataOperations(memory, counter)

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
          // Stack operations
          case POP   => memory.pop()
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
          case PRIMITIVE_PUT       => heapOperations.primitivePut()
          case PRIMITIVE_GET     => heapOperations.primitiveGet()
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
          case CAST => dataOperations.cast()
          case CONCAT => dataOperations.concat()
          case SLICE => dataOperations.slice()
          // System operations
          case STOP    => continue = false
          case FROM    => systemOperations.from()
          case LCALL   => systemOperations.lcall()
          case PCREATE => systemOperations.pcreate()
          case PUPDATE => systemOperations.pupdate()
          case PADDR   => systemOperations.paddr()
          case META    => Meta.readFromByteBuffer(program)
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
