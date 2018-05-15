package io.mytc.sood
package vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString

import scala.annotation.{strictfp, switch, tailrec}
import scala.collection.mutable.ArrayBuffer
import state._
import serialization._
import state.VmError._

object Vm {

  import Opcodes.int._

  val loader: Loader = DefaultLoader

  def runRaw(program: ByteString, executor: Address, environment: Environment): Memory = run(
    program = ByteBuffer.wrap(program.toByteArray),
    environment = environment,
    memory = Memory.empty,
    executor = executor,
    progAddress = None,
    progStorage = None,
    depth = 0,
    isLibrary = false
  )

  def runProgram(programAddress: Address,
                 initMemory: Memory = Memory.empty,
                 executor: Address,
                 environment: Environment,
                 depth: Int = 0): Memory = {

    val account = environment.getProgram(programAddress)
    if (account.isEmpty) throw VmErrorException(NoSuchProgram)

    val program = account.get.code
    program.rewind()
    run(program, environment, initMemory, executor, Some(programAddress), Some(account.get.storage), depth, isLibrary = false)
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
                   isLibrary: Boolean
  ): Memory = {

    lazy val storage = {
      if (progStorage.isEmpty) throw VmErrorException(OperationDenied)
      progStorage.get
    }

    var currentPosition = program.position()

    val callStack = new ArrayBuffer[Int](1024)

    def callPop(): Int = {
      callStack.remove(callStack.length - 1)
    }

    def callPush(pos: Int): Unit = {
      callStack += pos
    }

    @tailrec
    @strictfp
    def aux(): Memory =
      if (program.hasRemaining) {
        currentPosition = program.position()

        (program.get() & 0xff: @switch) match {
          case CALL =>
            callPush(program.position())
            program.position(dataToInt32(memory.pop()))
            aux()
          case RET =>
            if (callStack.isEmpty) {
              memory
            } else {
              program.position(callPop())
              aux()
            }
          case PCALL =>
            if (isLibrary) {
              throw VmErrorException(OperationDenied)
            }
            // FIXME this values can be produced my runtime
            val address = wordToData(program)
            val num = wordToInt32(program)
            val mem = runProgram(address, memory.top(num), executor, environment, depth + 1)
            memory ++= mem
            aux()
          case PCREATE =>
            memory.push(environment.createProgram(executor, memory.pop()))
            aux()
          case PUPDATE =>
            val address = memory.pop()
            val code = memory.pop()
            if (address != executor) {
              throw VmErrorException(OperationDenied)
            }
            environment.updateProgram(address, code)
            aux()
          case LCALL =>
            val address = wordToData(program)
            val func = wordToData(program)
            val num = wordToInt32(program)
            val callData = memory.top(num)

            loader.lib(address, environment) match {
              case None => throw VmErrorException(NoSuchLibrary)
              case Some(library) =>
                library.func(func) match {
                  case None => throw VmErrorException(NoSuchMethod)
                  case Some(function) =>
                    function match {
                      case f: StdFunction => memory ++= f(callData)
                      case UserDefinedFunction(f) =>
                        memory ++= run(f, environment, callData, executor, Some(address), None, depth + 1, isLibrary = true)
                    }
                }
            }

            aux()
          case JUMP =>
            program.position(dataToInt32(memory.pop()))
            aux()
          case JUMPI =>
            val condition = memory.pop()
            val position = memory.pop()
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
            memory.push(word.substring(from, until))
            aux()
          case CONCAT =>
            memory.push(memory.pop().concat(memory.pop()))
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
            memory.push(memory.stack(memory.stack.length - n))
            aux()
          case SWAP =>
            val fsti = memory.stack.length - 1
            val sndi = fsti - 1
            val fst = memory.stack(fsti)
            val snd = memory.stack(sndi)
            memory.stack(fsti) = snd
            memory.stack(sndi) = fst
            aux()
          case SWAPN =>
            val n = dataToInt32(memory.pop())
            val fsti = memory.stack.length - 1
            val sndi = memory.stack.length - n
            val fst = memory.stack(fsti)
            val snd = memory.stack(sndi)
            memory.stack(fsti) = snd
            memory.stack(sndi) = fst
            aux()
          case MPUT =>
            val i = memory.heap.length
            memory.heap += memory.pop()
            memory.push(int32ToData(i))
            aux()
          case MGET =>
            val i = dataToInt32(memory.pop())
            memory.push(memory.heap(i))
            aux()
          case SPUT =>
            val value = memory.pop()
            val key = memory.pop()
            storage.put(key, value)
            aux()
          case SGET =>
            memory.push(storage.get(memory.pop()).get)
            aux()
          case SDROP =>
            storage.delete(memory.pop())
            aux()
          case I32ADD =>
            memory.push(int32ToData(dataToInt32(memory.pop()) + dataToInt32(memory.pop())))
            aux()
          case I32MUL =>
            memory.push(int32ToData(dataToInt32(memory.pop()) * dataToInt32(memory.pop())))
            aux()
          case I32DIV =>
            memory.push(int32ToData(dataToInt32(memory.pop()) / dataToInt32(memory.pop())))
            aux()
          case I32MOD =>
            memory.push(int32ToData(dataToInt32(memory.pop()) % dataToInt32(memory.pop())))
            aux()
          case FADD =>
            memory.push(doubleToData(dataToDouble(memory.pop()) + dataToDouble(memory.pop())))
            aux()
          case FMUL =>
            memory.push(doubleToData(dataToDouble(memory.pop()) * dataToDouble(memory.pop())))
            aux()
          case FDIV =>
            memory.push(doubleToData(dataToDouble(memory.pop()) / dataToDouble(memory.pop())))
            aux()
          case FMOD =>
            memory.push(doubleToData(dataToDouble(memory.pop()) % dataToDouble(memory.pop())))
            aux()
          case NOT =>
            memory.push(boolToData(!dataToBool(memory.pop())))
            aux()
          case AND =>
            val left = memory.pop()
            val right = memory.pop()
            memory.push(
              boolToData(dataToBool(left) && dataToBool(right))
            )
            aux()
          case OR =>
            val left = memory.pop()
            val right = memory.pop()
            memory.push(
              boolToData(dataToBool(left) || dataToBool(right))
            )
            aux()
          case XOR =>
            val left = memory.pop()
            val right = memory.pop()
            memory.push(
              boolToData(dataToBool(left) ^ dataToBool(right))
            )
            aux()
          case EQ =>
            memory.push(boolToData(memory.pop() == memory.pop()))
            aux()
          case I32LT =>
            val d1 = dataToInt32(memory.pop())
            val d2 = dataToInt32(memory.pop())
            memory.push(boolToData(d1 < d2))
            aux()
          case I32GT =>
            val d1 = dataToInt32(memory.pop())
            val d2 = dataToInt32(memory.pop())
            memory.push(boolToData(d1 > d2))
            aux()
          case FROM =>
            memory.push(executor)
            aux()
          case STOP => memory
        }
      } else {
        memory
      }

    try {
      aux()
    } catch {
      case err: VmErrorException => throw err.addToTrace(Point(callStack, currentPosition, progAddress))
      case other: Exception =>
        throw VmErrorException(SomethingWrong(other)).addToTrace(Point(callStack, program.position(), progAddress))
    }

  }

}
