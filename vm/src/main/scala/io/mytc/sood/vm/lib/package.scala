package io.mytc.sood.vm

import state.Memory

package object lib {

  type Func = (Memory) => Memory

  type FuncTable = Map[Byte, Func]
  type LibTable = Map[Byte, FuncTable]


  object libTable {
    def apply(pair: (Byte, FuncTable)*): LibTable = {
      Map(pair:_*)
    }
  }

  object funcTable {
    def apply(pair: (std.StdFunc, Func)*): FuncTable = {
      Map(pair.map{case (f, v) => (f.fcode, v)}:_*)
    }
  }

  trait Lib {
    val code: Byte
    val table: FuncTable
    val lib: (Byte, FuncTable) = code -> table
  }

  val StdLib = libTable(
    Math.lib,
    Hash.lib
  )

}
