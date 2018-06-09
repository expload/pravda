package pravda

package vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.domain.Address
import pravda.vm.watt.{EnvironmentWithCounter, MemoryWithCounter, StorageWithCounter, WattCounter}

import scala.annotation.{strictfp, switch, tailrec}
import scala.collection.mutable.ArrayBuffer
import state._
import serialization._
import state.VmError._
import WattCounter._

import scala.util.{Failure, Success, Try}

object Vm {

  import Opcodes.int._

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
            program.position(dataToInt32(memory.pop()))
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
            val num = dataToInt32(memory.pop())
            val address = dataToAddress(memory.pop())
            memory.limit(num)
            runProgram(address, memory, executor, environment, depth + 1, wattCounter)
            memory.dropLimit()
            aux()
          case PCREATE =>
            memory.push(environment.createProgram(executor, memory.pop()))
            aux()
          case PUPDATE =>
            val address = dataToAddress(memory.pop())
            val code = memory.pop()
            if (address != executor) {
              throw VmErrorException(OperationDenied)
            }
            environment.updateProgram(address, code)
            aux()
          case PADDR =>
            if (progAddress.isEmpty) {
              throw VmErrorException(OperationDenied)
            } else {
              memory.push(addressToData(progAddress.get))
            }
            aux()
          case TRANSFER =>
            val amount = dataToCoins(memory.pop())
            val to = dataToAddress(memory.pop())
            environment.transfer(executor, to, amount)
            aux()
          case PTRANSFER =>
            if (progAddress.isEmpty) {
              throw VmErrorException(OperationDenied)
            } else {
              val amount = dataToCoins(memory.pop())
              val to = dataToAddress(memory.pop())
              environment.transfer(progAddress.get, to, amount)
            }
            aux()
          case LCALL =>
            wattCounter.cpuUsage(CpuExtCall)

            val address = wordToAddress(program)
            val func = wordToData(program)
            val num = wordToInt32(program)
            memory.limit(num)

            loader.lib(address, environment) match {
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
                            Some(address),
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

            program.position(dataToInt32(memory.pop()))
            aux()
          case JUMPI =>
            wattCounter.cpuUsage(CpuSimpleArithmetic, CpuProgControl)

            val position = memory.pop()
            val condition = memory.pop()
            if (dataToBool(condition))
              program.position(dataToInt32(position))
            aux()
          case PUSHX =>
            memory.push(wordToData(program))
            aux()
          case SLICE =>
            val from = wordToInt32(program)
            val until = wordToInt32(program)
            val word = memory.pop()

            wattCounter.cpuUsage(CpuWordOperation(word))

            memory.push(word.substring(from, until))
            aux()
          case CONCAT =>
            val word1 = memory.pop()
            val word2 = memory.pop()

            wattCounter.cpuUsage(CpuWordOperation(word1, word2))

            memory.push(word1.concat(word2))
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
            val n = dataToInt32(memory.pop())
            memory.push(memory.get(memory.length - n))
            aux()
          case SWAP =>
            val fsti = memory.length - 1
            val sndi = fsti - 1
            memory.swap(fsti, sndi)
            aux()
          case SWAPN =>
            val n = dataToInt32(memory.pop())
            val fsti = memory.length - 1
            val sndi = memory.length - n
            memory.swap(fsti, sndi)
            aux()
          case MPUT =>
            val i = memory.heapPut(memory.pop())
            memory.push(int32ToData(i))
            aux()
          case MGET =>
            val i = dataToInt32(memory.pop())
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
            memory.push(boolToData(storage.get(dataToAddress(memory.pop())).isDefined))
            aux()
          case I32ADD =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)

            memory.push(int32ToData(dataToInt32(memory.pop()) + dataToInt32(memory.pop())))
            aux()
          case I32MUL =>
            wattCounter.cpuUsage(CpuArithmetic)

            memory.push(int32ToData(dataToInt32(memory.pop()) * dataToInt32(memory.pop())))
            aux()
          case I32DIV =>
            wattCounter.cpuUsage(CpuArithmetic)

            memory.push(int32ToData(dataToInt32(memory.pop()) / dataToInt32(memory.pop())))
            aux()
          case I32MOD =>
            wattCounter.cpuUsage(CpuArithmetic)

            memory.push(int32ToData(dataToInt32(memory.pop()) % dataToInt32(memory.pop())))
            aux()
          case FADD =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)

            memory.push(doubleToData(dataToDouble(memory.pop()) + dataToDouble(memory.pop())))
            aux()
          case FMUL =>
            wattCounter.cpuUsage(CpuArithmetic)

            memory.push(doubleToData(dataToDouble(memory.pop()) * dataToDouble(memory.pop())))
            aux()
          case FDIV =>
            wattCounter.cpuUsage(CpuArithmetic)

            memory.push(doubleToData(dataToDouble(memory.pop()) / dataToDouble(memory.pop())))
            aux()
          case FMOD =>
            wattCounter.cpuUsage(CpuArithmetic)

            memory.push(doubleToData(dataToDouble(memory.pop()) % dataToDouble(memory.pop())))
            aux()
          case NOT =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)

            memory.push(boolToData(!dataToBool(memory.pop())))
            aux()
          case AND =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)

            val left = memory.pop()
            val right = memory.pop()
            memory.push(
              boolToData(dataToBool(left) && dataToBool(right))
            )
            aux()
          case OR =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)

            val left = memory.pop()
            val right = memory.pop()
            memory.push(
              boolToData(dataToBool(left) || dataToBool(right))
            )
            aux()
          case XOR =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)

            val left = memory.pop()
            val right = memory.pop()
            memory.push(
              boolToData(dataToBool(left) ^ dataToBool(right))
            )
            aux()
          case EQ =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)

            memory.push(boolToData(memory.pop() == memory.pop()))
            aux()
          case I32LT =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)

            val d1 = dataToInt32(memory.pop())
            val d2 = dataToInt32(memory.pop())
            memory.push(boolToData(d1 < d2))
            aux()
          case I32GT =>
            wattCounter.cpuUsage(CpuSimpleArithmetic)

            val d1 = dataToInt32(memory.pop())
            val d2 = dataToInt32(memory.pop())
            memory.push(boolToData(d1 > d2))
            aux()
          case FROM =>
            memory.push(executor)
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
