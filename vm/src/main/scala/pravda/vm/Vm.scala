package pravda

package vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.domain.Address
import pravda.vm.watt.{EnvironmentWithCounter, MemoryWithCounter, StorageWithCounter, WattCounter}

import scala.annotation.{strictfp, switch, tailrec}
import scala.collection.mutable.ArrayBuffer
import state._
import state.VmError._
import WattCounter._

import scala.util.{Failure, Success, Try}

object Vm {

  import Opcodes.int._
  import Data._
  import DataOperations._

  val loader: Loader = DefaultLoader

  def runRaw(program: ByteString, executor: Address, environment: Environment, wattLimit: Long): ExecutionResult = {
    val wattCounter = new WattCounter(wattLimit)
    val memory = VmMemory.empty
    Try {
      run(
        program = ByteBuffer.wrap(program.toByteArray),
        environment = EnvironmentWithCounter(environment, wattCounter),
        memory = MemoryWithCounter(memory, wattCounter),
        executor = executor,
        progAddress = None,
        progStorage = None,
        depth = 0,
        isLibrary = false,
        wattCounter = wattCounter
      )
    } match {
      case Success(_)                     => ExecutionResult(memory, None, wattCounter)
      case Failure(err: VmErrorException) => ExecutionResult(memory, Some(err), wattCounter)
      case Failure(err)                   => ExecutionResult(memory, Some(VmErrorException(SomethingWrong(err))), wattCounter)
    }
  }

  def runProgram(programAddress: Address,
                 initMemory: Memory,
                 executor: Address,
                 environment: Environment,
                 depth: Int = 0,
                 wattCounter: WattCounter): Unit = {

    val account = environment.getProgram(programAddress)
    if (account.isEmpty) throw VmErrorException(NoSuchProgram)

    val program = account.get.code
    program.rewind()
    run(program,
        environment,
        initMemory,
        executor,
        Some(programAddress),
        Some(account.get.storage),
        depth,
        isLibrary = false,
        wattCounter = wattCounter)
  }

  // TODO @fomkin: looks like isLibrary and emptiness of storage are the same things.
  private def run(
      program: ByteBuffer,
      environment: Environment,
      memory: Memory,
      executor: Address,
      progAddress: Option[Address],
      progStorage: Option[Storage],
      depth: Int,
      isLibrary: Boolean,
      wattCounter: WattCounter
  ): Unit = {

    if (depth > 1024) throw VmErrorException(ExtCallStackOverflow)

    lazy val storage = {
      if (progStorage.isEmpty) throw VmErrorException(OperationDenied)
      new StorageWithCounter(progStorage.get, wattCounter)
    }

    var currentPosition = program.position()

    val callStack = new ArrayBuffer[Int](1024)

    def callPop(): Int = {
      callStack.remove(callStack.length - 1)
    }

    def callPush(pos: Int): Unit = {
      callStack += pos

      if (callStack.size > 1024) throw VmErrorException(CallStackOverflow)
    }

    @tailrec
    @strictfp
    def aux(): Unit =
      if (program.hasRemaining) {
        wattCounter.cpuUsage(CpuBasic)

        currentPosition = program.position()
        (program.get() & 0xff: @switch) match {
          case CALL =>
            wattCounter.cpuUsage(CpuProgControl)

            callPush(program.position())
            program.position(int32(memory.pop()))
            aux()
          case RET =>
            if (callStack.nonEmpty) {
              program.position(callPop())
              aux()
            }
          case PCALL =>
            wattCounter.cpuUsage(CpuExtCall)

            if (isLibrary) {
              throw VmErrorException(OperationDenied)
            }
            val num = int32(memory.pop())
            val addr = address(memory.pop())
            memory.limit(num)
            runProgram(addr, memory, executor, environment, depth + 1, wattCounter)
            memory.dropLimit()
            aux()
          case PCREATE =>
            val code = memory.pop()
            val programAddress = environment.createProgram(executor, code)
            memory.push(address(programAddress))
            aux()
          case PUPDATE =>
            val addr = address(memory.pop())
            val code = memory.pop()
            if (addr != executor) {
              throw VmErrorException(OperationDenied)
            }
            environment.updateProgram(addr, code)
            aux()
          case PADDR =>
            if (progAddress.isEmpty) {
              throw VmErrorException(OperationDenied)
            } else {
              memory.push(address(progAddress.get))
            }
            aux()
          case TRANSFER =>
            val amount = coin(memory.pop())
            val to = address(memory.pop())
            environment.transfer(executor, to, amount)
            aux()
          case PTRANSFER =>
            if (progAddress.isEmpty) {
              throw VmErrorException(OperationDenied)
            } else {
              val amount = coin(memory.pop())
              val to = address(memory.pop())
              environment.transfer(progAddress.get, to, amount)
            }
            aux()
          case LCALL =>
            wattCounter.cpuUsage(CpuExtCall)

            // FIXME should be popped from stack
            val addr = address(Data.readFromByteBuffer(program))
            val func = bytes(Data.readFromByteBuffer(program))
            val num = int32(Data.readFromByteBuffer(program))

            memory.limit(num)

            loader.lib(addr, environment) match {
              case None => throw VmErrorException(NoSuchLibrary)
              case Some(library) =>
                library.func(func) match {
                  case None => throw VmErrorException(NoSuchMethod)
                  case Some(function) =>
                    function match {
                      case f: StdFunction => f(memory, wattCounter)
                      case f: UserDefinedFunction =>
                        run(f.code,
                            environment,
                            memory,
                            executor,
                            Some(addr),
                            None,
                            depth + 1,
                            isLibrary = true,
                            wattCounter)
                    }
                }
            }
            memory.dropLimit()
            aux()
          case JUMP =>
            wattCounter.cpuUsage(CpuProgControl)
            program.position(int32(memory.pop()))
            aux()
          case JUMPI =>
            wattCounter.cpuUsage(CpuSimpleArithmetic, CpuProgControl)
            val position = int32(memory.pop())
            val condition = boolean(memory.pop())
            if (condition) program.position(position)
            aux()
          case PUSHX =>
            val data = Data.readFromByteBuffer(program)
            memory.push(data)
            aux()
          case SLICE =>
            // FIXME params should be popped from stack
            val from = int32(Data.readFromByteBuffer(program))
            val until = int32(Data.readFromByteBuffer(program))
            val data = memory.pop()
            val sliced = slice(data, from, until)
            wattCounter.cpuUsage(CpuWordOperation(data))
            memory.push(sliced)
            aux()
          case CONCAT =>
            val left = memory.pop()
            val right = memory.pop()
            wattCounter.cpuUsage(CpuWordOperation(left, right))
            memory.push(concat(left, right))
            aux()
          case POP =>
            memory.pop()
            aux()
          case DUP =>
            val x = memory.pop()
            memory.push(x)
            memory.push(x)
            aux()
          case DUPN =>
            val n = int32(memory.pop())
            memory.push(memory.get(memory.length - n))
            aux()
          case SWAP =>
            val fsti = memory.length - 1
            val sndi = fsti - 1
            memory.swap(fsti, sndi)
            aux()
          case SWAPN =>
            val n = int32(memory.pop())
            val fsti = memory.length - 1
            val sndi = memory.length - n
            memory.swap(fsti, sndi)
            aux()
          case MPUT =>
            val i = memory.heapPut(memory.pop())
            memory.push(Data.Primitive.Int32(i))
            aux()
          case MGET =>
            val i = int32(memory.pop())
            memory.push(memory.heapGet(i))
            aux()
          case SPUT =>
            val value = memory.pop()
            val key = memory.pop()
            storage.put(key, value)
            aux()
          case SGET =>
            val valOpt = storage.get(memory.pop())
            if (valOpt.isEmpty) {
              throw VmErrorException(NoSuchElement)
            }
            memory.push(valOpt.get)
            aux()
          case SDROP =>
            storage.delete(memory.pop())
            aux()
          case SEXIST =>
            val key = memory.pop()
            val defined = storage.get(key).isDefined
            memory.push(Primitive.Bool(defined))
            aux()
          case ADD =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)
            val left = memory.pop()
            val right = memory.pop()
            val r = add(left, right)
            memory.push(r)
            aux()
          case MUL =>
            wattCounter.cpuUsage(CpuArithmetic)
            val left = memory.pop()
            val right = memory.pop()
            val r = mul(left, right)
            memory.push(r)
            aux()
          case DIV =>
            wattCounter.cpuUsage(CpuArithmetic)
            val left = memory.pop()
            val right = memory.pop()
            val r = div(left, right)
            memory.push(r)
            aux()
          case MOD =>
            wattCounter.cpuUsage(CpuArithmetic)
            val left = memory.pop()
            val right = memory.pop()
            val r = mod(left, right)
            memory.push(r)
            aux()
          case NOT =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)
            memory.push(not(memory.pop()))
            aux()
          case AND =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)
            val left = memory.pop()
            val right = memory.pop()
            val r = and(left, right)
            memory.push(r)
            aux()
          case OR =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)
            val left = memory.pop()
            val right = memory.pop()
            val r = or(left, right)
            memory.push(r)
            aux()
          case XOR =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)
            val left = memory.pop()
            val right = memory.pop()
            val r = xor(left, right)
            memory.push(r)
            aux()
          case EQ =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)
            memory.push(Primitive.Bool(memory.pop() == memory.pop()))
            aux()
          case LT =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)
            val a = memory.pop()
            val b = memory.pop()
            val r = lt(a, b)
            memory.push(r)
            aux()
          case GT =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)
            val a = memory.pop()
            val b = memory.pop()
            val r = gt(a, b)
            memory.push(r)
            aux()
          case FROM =>
            memory.push(address(executor))
            aux()
          case STOP => ()
        }
      }

    try {
      aux()
    } catch {
      case err: VmErrorException =>
        throw err.addToTrace(Point(callStack, currentPosition, progAddress))
      case other: Throwable =>
        throw VmErrorException(SomethingWrong(other)).addToTrace(Point(callStack, program.position(), progAddress))
    }

  }

}
