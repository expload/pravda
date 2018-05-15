package io.mytc.sood.vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import io.mytc.sood.vm.state._
import serialization._

object VmUtils {

  val emptyState: WorldState = new WorldState {
    override def get(address: Address): Option[AccountState] = None
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

  // FIXME lead to bugs
  /** They are NOT integers, they are bytes! */
  def binaryData(i: Int*): Data =
    ByteString.copyFrom(i.map(_.toByte).toArray)

  def data(i: Int): Data = {
    int32ToData(i)
  }

  def data(b: Byte): Data = {
    ByteString.copyFrom(Array(b))
  }

  def int(d: Data): Int = {
    dataToInt32(d)
  }

  def worldState(accs: (Address, Program)*): WorldState = new WorldState {

    def account(prog: Program): AccountState = new AccountState {
      override def program: ByteBuffer = prog.buffer
      override def storage: Storage = null // scalafix:ok
    }

    override def get(address: Address): Option[AccountState] = {
      accs.find{_._1 == address}.map {
        case (addr, prog) => account(prog)
      }
    }
  }

}
