package pravda.vm

package udf

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.domain.Address
import state.{Data, Environment}

import scala.collection.mutable

object Loader extends Loader {

  import DataOperations._

  type ExternalTable = mutable.Map[ByteString, Int]

  private def readExternalTable(program: ByteBuffer): ExternalTable = {
    val table: ExternalTable = mutable.Map.empty
    val n = int32(Data.readFromByteBuffer(program))
    for (i <- 1 to n) {
      val address = bytes(Data.readFromByteBuffer(program))
      val position = int32(Data.readFromByteBuffer(program))
      table += (address -> position)
    }
    table
  }

  override def lib(address: Address, worldState: Environment): Option[Library] = {
    worldState.getProgram(address).flatMap { acc =>
      val program = acc.code
      program.rewind()

      if (program.get() == Opcodes.FTBL) {
        val table: ExternalTable = readExternalTable(program)
        val lib = new Library {
          override def func(name: ByteString): Option[Function] = {
            table.get(name).map { i =>
              val prog = program.duplicate()
              prog.position(i)
              Func(prog)
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
