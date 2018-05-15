package io.mytc.sood.vm
package udf

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import state.{Address, WorldState}

import scala.collection.mutable

object Loader extends Loader {
  type ExternalTable = mutable.Map[ByteString, Int]

  private def readExternalTable(program: ByteBuffer): ExternalTable = {
    val table: ExternalTable = mutable.Map.empty
    val n = wordToInt32(program)
    for (i <- 1 to n) {
      val address = wordToBytes(program)
      val position = wordToInt32(program)
      table += (ByteString.copyFrom(address) -> position)
    }
    table
  }

  override def lib(address: Address, worldState: WorldState): Option[Library] = {
    worldState.get(address).flatMap { acc =>
      val program = acc.program
      program.rewind()

      if (program.get() == Opcodes.FTBL) {
        val table: ExternalTable = readExternalTable(program)
        val lib = new Library {
          override def func(name: ByteString): Option[Function] = {
            table.get(name).map { i =>
              val prog = program.duplicate()
              prog.position(i)
              UserDefinedFunction(prog)
            }
          }
        }
        Some(lib)
      } else {
        None
      }
    }
  }

}
