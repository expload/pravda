package io.mytc.sood
package libs

import java.nio.ByteBuffer

import vm.state.WorldState
import vm._

object Loader extends Loader {
  def readExternalTable(program: ByteBuffer): ExternalTable = {
    val n = wordToInt32(program)
    val table = ExternalTable()
    for (i <- 1 to n) {
      val address = wordToBytes(program)
      val position = wordToInt32(program)
      table.put(address, position)
    }
    table
  }

  override def lib(address: Array[Byte], worldState: WorldState): Option[Library] = {
    val program = worldState.get(address).program
    if(program.get() == Opcodes.FTBL) {
      val table: ExternalTable = readExternalTable(program)
      val lib  = new Library {
        override def func(name: Array[Byte]): Option[Function] = {
          table.get(name).map(i =>
            LibFunction(code = program.position(i).asInstanceOf[ByteBuffer])
          )
        }
      }
      Some(lib)
    } else {
      None
    }
  }

}
