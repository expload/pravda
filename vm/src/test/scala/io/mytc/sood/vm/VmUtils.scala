package io.mytc.sood.vm

import java.nio.ByteBuffer
import java.util

import io.mytc.sood.vm.state._
import serialization._

object VmUtils {

  val emptyState = new WorldState {
    override def get(address: Address): AccountState = ???
  }

  def exec(p: Program): Array[Data] = {
    Vm.runTransaction(p.buffer, emptyState).stack.toArray
  }

  def exec(p: Program, worldState: WorldState): Array[Data] = {
    Vm.runTransaction(p.buffer, worldState).stack.toArray
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

  def data(v: Double): Data = {
    doubleToData(v)
  }

  def data(i: Int): Data = {
    int32ToData(i)
  }

  def data(b: Byte): Data = {
    Array(b)
  }

  def int(d: Data): Int = {
    dataToInt32(d)
  }


  def worldState(accs: (Address, Program)*) = new WorldState {

    def account(prog: Program): AccountState = new AccountState {
      override def program: ByteBuffer = prog.buffer
      override def storage: Storage = null
    }

    override def get(address: Address): AccountState = {
      accs.find{case (addr, _) => util.Arrays.equals(addr, address)}.map{
        case (addr, prog) => account(prog)
      }.get
    }
  }

}
