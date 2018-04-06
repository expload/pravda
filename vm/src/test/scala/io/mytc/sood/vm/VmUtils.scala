package io.mytc.sood.vm

import io.mytc.sood.vm.Vm.Word

import scala.collection.mutable.ArrayBuffer

object VmUtils {

  def exec(p: Program): Array[Word] = {
    Vm.run(p.buffer, Option(p.stack.to[ArrayBuffer])).toArray
  }

  def stack(words: Word*): Array[Word] =  words.toArray

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

}
