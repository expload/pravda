package io.mytc.sood.vm
package lib

import state.Memory

import std.Hash._

object Hash extends Lib {

  def sha3: Func = (memory: Memory) => {
    // Mock
    val p = memory.pop()
    memory.push(p)
    memory
  }

  override val table: FuncTable = funcTable(
    SHA3 -> sha3
  )
  override val code: Byte = CODE

}
