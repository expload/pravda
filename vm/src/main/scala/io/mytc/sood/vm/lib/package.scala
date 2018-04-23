package io.mytc.sood.vm

import state.Memory

package object lib {

  type Func = (Memory) => Unit

  type FuncTable = Map[Byte, Func]
  type LibTable = Map[Byte, FuncTable]


  object libTable {
    def apply(pair: (Int, FuncTable)*): LibTable = {
      Map(pair.map{case (k, v) => (k.toByte, v)}:_*)
    }
  }

  object funcTable {
    def apply(pair: (Int, Func)*): FuncTable = {
      Map(pair.map{case (k, v) => (k.toByte, v)}:_*)
    }
  }

  trait Lib {
    val table: FuncTable
  }

  val StdLib = libTable(
    0 -> Math.table,
    1 -> Hash.table
  )
}
