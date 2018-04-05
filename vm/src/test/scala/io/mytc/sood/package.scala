package io.mytc

import io.mytc.sood.vm.Vm
import io.mytc.sood.vm.Vm.Word
import org.scalatest._

import scala.collection.mutable.ArrayBuffer

package object sood extends Matchers {

  def exec(p: Program): Array[Word] = {
    Vm.run(p.buffer, Option(p.stack.to[ArrayBuffer])).toArray
  }

  def stack(words: Word*): Array[Word] =  words.toArray
  def prog: Program = Program()

}
