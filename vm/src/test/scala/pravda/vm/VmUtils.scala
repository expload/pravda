package pravda.vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.vm.state._
import serialization._

object VmUtils {

  val emptyState: Environment = new Environment {
    def getProgram(address: Address): Option[ProgramContext] = None
    def getProgramOwner(address: Address): Option[Address] = ???
    def createProgram(owner: Address, code: Data): Address = ???
    def updateProgram(address: Data, code: Data): Unit = ???
  }

  def exec(p: ProgramStub): Array[Data] = {
    Vm.runRaw(p.byteString, ByteString.EMPTY, emptyState).stack.toArray
  }

  def exec(p: ProgramStub, worldState: Environment): Array[Data] = {
    Vm.runRaw(p.byteString, ByteString.EMPTY, worldState).stack.toArray
  }

  def stack(item: Data*): Array[Data] =  item.toArray

  def prog: ProgramStub = ProgramStub()

  def hex(b: Byte): String = {
    val s = (b & 0xFF).toHexString
    if(s.length < 2){
      s"0$s"
    } else {
      s
    }
  }

  def hex(bs: Seq[Byte]): String = {
    bs.map(hex).mkString(" ")
  }

  def data(v: Double): Data = {
    doubleToData(v)
  }

  // FIXME lead to bugs
  /** They are NOT integers, they are bytes! */
  def binaryData(i: Int*): Data =
    ByteString.copyFrom(i.map(_.toByte).toArray)

  def data(i: Int): Data = {
    int32ToData(i)
  }

  def data(b: Byte): Data = {
    ByteString.copyFrom(Array(b))
  }

  def int(d: Data): Int = {
    dataToInt32(d)
  }

  def environment(accs: (Address, ProgramStub)*): Environment = new Environment {

    def program(prog: ProgramStub): ProgramContext = new ProgramContext {
      def code: ByteBuffer = prog.buffer
      def storage: Storage = null // scalafix:ok
    }

    def getProgram(address: Address): Option[ProgramContext] = {
      accs.find{_._1 == address}.map {
        case (addr, prog) => program(prog)
      }
    }

    def getProgramOwner(address: Address): Option[Address] = ???

    def createProgram(owner: Address, code: Data): Address = ???

    def updateProgram(address: Data, code: Data): Unit = ???

  }

}
