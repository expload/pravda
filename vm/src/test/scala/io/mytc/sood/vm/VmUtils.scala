package io.mytc.sood.vm

import java.nio.ByteBuffer

import io.mytc.sood.vm.state.{AccountState, Address, Data, WorldState}
import serialization._

object VmUtils {

  val emptyState = new WorldState {
    override def get(address: Address): AccountState = ???
  }

  def exec(p: Program): Array[Data] = {
    val program = p.stack.flatMap(w => Array(Opcodes.PUSHX) ++ bytesToWord(w)).toArray ++ p.bytes
    Vm.runTransaction(ByteBuffer.wrap(program), emptyState).stack.toArray
  }

  def stack(item: Data*): Array[Data] =  item.toArray

  def prog: Program = Program()

  def hex(b: Byte): String = {
    val s = (b & 0xFF).toHexString
    if(s.length < 2){
      s"0$s"
    } else {
      s
    }
  }

  def hex(bs: Seq[Byte]): String = {
    bs.map(hex).mkString(" ")
  }

  def data(i: Int): Data = {
    int32ToData(i)
  }

  def int(d: Data): Int = {
    dataToInt32(d)
  }
}
