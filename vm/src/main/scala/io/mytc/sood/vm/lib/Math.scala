package io.mytc.sood.vm
package lib

import state.Memory
import serialization._

object Math extends Lib {

  def sum(mem: Memory): Unit = {
    mem.push(int32ToData(2))
    sumN(mem)
  }

  def sumN(mem: Memory): Unit = {
    val n = dataToInt32(mem.pop())
    var sum = 0
    for(i <- 0 to n) {
      sum += dataToInt32(mem.pop())
    }
    mem.push(int32ToData(sum))
  }

  override val table: FuncTable = funcTable(
    0 -> sum,
    1 -> sumN
  )

}
