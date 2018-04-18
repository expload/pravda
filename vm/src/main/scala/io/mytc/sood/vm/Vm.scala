package io.mytc.sood.vm

import java.nio.ByteBuffer

import scala.annotation.{switch, tailrec}
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

    def callPop(): Int = {
      callStack.remove(callStack.length - 1)
    }

    def callPush(pos: Int): Unit = {
      callStack += pos
    }

    @tailrec def aux(): Unit = if (program.hasRemaining) {
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
          val address = mem.pop()
          mem = runProgram(address, mem, worldState, depth)
          aux()
        case JUMP =>
          program.position(dataToInt32(mem.pop()))
          aux()
        case JUMPI =>
          if (mem.pop().exists(_ != 0.toByte))
            program.position(dataToInt32(mem.pop()))
          else mem.pop()
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
        case NOT =>
          mem.push(bytewise(mem.pop())(~ _))
          aux()
        case AND =>
          mem.push(
            bytewise(mem.pop(), mem.pop())(_ & _)(0.toByte)
          )
          aux()
        case OR =>
          mem.push(
            bytewise(mem.pop(), mem.pop())(_ | _)(0xFF.toByte)
          )
          aux()
        case XOR =>
          mem.push(
            bytewise(mem.pop(), mem.pop())(_ ^ _)(0.toByte)
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

  val FALSE: Data = Array(0.toByte)
  val TRUE: Data = Array(1.toByte)

  def boolToData(b: Boolean): Data = {
    if(b) TRUE else FALSE
  }

  def bytewise(d: Data)(f: (Byte) => Int): Data = {
    d.map(f(_).toByte)
  }

  def bytewise(d1: Data, d2: Data)(f: (Byte, Byte) => Int)(neutral: Byte): Data = {
    val len = math.max(d1.length, d2.length)
    d1.reverseIterator.zipAll(d2.reverseIterator, neutral, neutral).map{
      case (b1, b2) => f(b1, b2).toByte
    }.toArray.reverse
  }
}
