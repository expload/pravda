package io.mytc.sood.vm
package lib

import state.Memory
import serialization._

import std.Math._

object Math extends Lib {

  def sum: Func = (mem: Memory) => {
    mem.push(int32ToData(2))
    sumN(mem)
  }

  def sumN: Func = (mem: Memory) => {
    val n = dataToInt32(mem.pop())
    var sum = 0
    for(i <- 0 to n) {
      sum += dataToInt32(mem.pop())
    }
    mem.push(int32ToData(sum))
    mem
  }

  override val table: FuncTable = funcTable(
    SUM -> sum,
    SUMN -> sumN
  )

  override val code: Byte = CODE

}
