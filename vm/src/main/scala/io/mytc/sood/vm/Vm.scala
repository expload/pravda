package io.mytc.sood
package vm

import java.nio.ByteBuffer

import scala.annotation.{switch, tailrec, strictfp}
import scala.collection.mutable.ArrayBuffer
import state._
import serialization._

object Vm {


  import Opcodes.int._

  def runTransaction(
                      program: ByteBuffer,
                      worldState: WorldState): Memory = {

    val initMemory = Memory.empty
    run(program, worldState, initMemory, None, 0)

  }

  def runProgram(
                  programAddress: Address,
                  initMemory: Memory = Memory.empty,
                  worldState: WorldState,
                  depth: Int = 0): Memory = {

    val account = worldState.get(programAddress)
    run(account.program, worldState, initMemory, Some(account.storage), depth + 1)

  }

  private def run(
                   program: ByteBuffer,
                   worldState: WorldState,
                   initMemory: Memory,
                   progStorage: Option[Storage],
                   depth: Int): Memory = {

    var mem = initMemory

    lazy val storage = progStorage.get

    val callStack = new ArrayBuffer[Int](1024)

    val loader: Loader = std.Libs

    def callPop(): Int = {
      callStack.remove(callStack.length - 1)
    }

    def callPush(pos: Int): Unit = {
      callStack += pos
    }

    @tailrec
    @strictfp
    def aux(): Unit = if (program.hasRemaining) {
      (program.get() & 0xff: @switch) match {
        case CALL =>
          callPush(program.position())
          program.position(dataToInt32(mem.pop()))
          aux()
        case RET =>
          if(callStack.nonEmpty) {
            program.position(callPop())
            aux()
          }
        case PCALL =>
          val address = wordToData(program)
          runProgram(address, mem, worldState, depth)
          aux()
        case DCALL =>
          val address = wordToData(program)
          val func = wordToData(program)
          loader.lib(address).flatMap(_.func(func).map(_.apply(mem)))
          aux()
        case JUMP =>
          program.position(dataToInt32(mem.pop()))
          aux()
        case JUMPI =>
          val condition = mem.pop()
          val position = mem.pop()
          if (dataToBool(condition))
            program.position(dataToInt32(position))
          aux()
        case PUSHX =>
          mem.push(wordToData(program))
          aux()
        case POP =>
          mem.pop()
          aux()
        case DUP =>
          val x = mem.pop()
          mem.push(x)
          mem.push(x)
          aux()
        case SWAP =>
          val fsti = mem.stack.length - 1
          val sndi = fsti - 1
          val fst = mem.stack(fsti)
          val snd = mem.stack(sndi)
          mem.stack(fsti) = snd
          mem.stack(sndi) = fst
          aux()
        case MPUT =>
          val i = mem.heap.length
          mem.heap += mem.pop()
          mem.push(int32ToData(i))
          aux()
        case MGET =>
          val i = dataToInt32(mem.pop())
          mem.push(mem.heap(i))
          aux()
        case SPUT =>
          val value = mem.pop()
          val key = mem.pop()
          storage.put(key, value)
          aux()
        case SGET =>
          mem.push(storage.get(mem.pop()).get)
          aux()
        case SDROP =>
          storage.delete(mem.pop())
          aux()
        case I32ADD =>
          mem.push(int32ToData(dataToInt32(mem.pop()) + dataToInt32(mem.pop())))
          aux()
        case I32MUL =>
          mem.push(int32ToData(dataToInt32(mem.pop()) * dataToInt32(mem.pop())))
          aux()
        case I32DIV =>
          mem.push(int32ToData(dataToInt32(mem.pop()) / dataToInt32(mem.pop())))
          aux()
        case I32MOD =>
          mem.push(int32ToData(dataToInt32(mem.pop()) % dataToInt32(mem.pop())))
          aux()
        case FADD =>
          mem.push(doubleToData(dataToDouble(mem.pop()) + dataToDouble(mem.pop())))
          aux()
        case FMUL =>
          mem.push(doubleToData(dataToDouble(mem.pop()) * dataToDouble(mem.pop())))
          aux()
        case FDIV =>
          mem.push(doubleToData(dataToDouble(mem.pop()) / dataToDouble(mem.pop())))
          aux()
        case FMOD =>
          mem.push(doubleToData(dataToDouble(mem.pop()) % dataToDouble(mem.pop())))
          aux()
        case NOT =>
          mem.push(boolToData(!dataToBool(mem.pop())))
          aux()
        case AND =>
          val left = mem.pop()
          val right = mem.pop()
          mem.push(
            boolToData(dataToBool(left) && dataToBool(right))
          )
          aux()
        case OR =>
          val left = mem.pop()
          val right = mem.pop()
          mem.push(
            boolToData(dataToBool(left) || dataToBool(right))
          )
          aux()
        case XOR =>
          val left = mem.pop()
          val right = mem.pop()
          mem.push(
            boolToData(dataToBool(left) ^ dataToBool(right))
          )
          aux()
        case EQ =>
          mem.push(boolToData(mem.pop().sameElements(mem.pop())))
          aux()
        case I32LT =>
          val d1 = dataToInt32(mem.pop())
          val d2 = dataToInt32(mem.pop())
          mem.push(boolToData(d1 < d2))
        case I32GT =>
          val d1 = dataToInt32(mem.pop())
          val d2 = dataToInt32(mem.pop())
          mem.push(boolToData(d1 > d2))
        case STOP => ()
      }
    }
    program.rewind()
    aux()
    mem
  }

}
