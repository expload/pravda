package io.mytc.sood

import io.mytc.sood.vm.Vm
import io.mytc.sood.vm.Vm.Word

package object serialize {

  def bytes(bs: Int*): Array[Byte] = bs.map(_.toByte).toArray

  final val LEN4 = 0xC0.toByte
  final val LEN3 = 0x80.toByte
  final val LEN2 = 0x40.toByte
  final val LEN1 = 0x00.toByte
  final val ZERO = 0x00.toByte

  def word(i: Int): Word = {
    val w = pureWord(i)
    w(0) = LEN4
    w
  }

  def word(b: Byte): Word = {
    val w = pureWord(b)
    w(0) = LEN1
    w
  }

  def pureWord(i: Int): Word = {
    Vm.int32ToWord(i)
  }

  def pureWord(b: Byte): Word = {
    val w = new Array[Byte](2)
    w(0) = ZERO
    w(1) = b
    w
  }

  def clearLength(w: Word): Word = {
    val res = w.clone()
    res(0) = (res(0) & 0x3F).toByte
    res
  }

}
