package io.mytc.sood.vm
package lib

import state.Memory

object Hash extends Lib {

  def sha3: Func = (memory: Memory) => {
    // Mock
    val p = memory.pop()
    memory.push(p)
  }

  override val table: FuncTable = funcTable(
    0 -> sha3
  )
}
