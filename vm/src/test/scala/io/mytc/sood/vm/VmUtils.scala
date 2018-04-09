package io.mytc.sood.vm

import io.mytc.sood.vm.Vm._

import scala.collection.mutable.ArrayBuffer

object VmUtils {

  def exec(p: Program): Array[Data] = {
    Vm.run(p.buffer, Option(p.stack.to[ArrayBuffer])).toArray
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
