package pravda.vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.domain.Address
import pravda.vm.state._

object VmUtils {

  val emptyState: Environment = new Environment {
    def getProgram(address: Address): Option[ProgramContext] = None
    def getProgramOwner(address: Address): Option[Address] = ???
    def createProgram(owner: Address, code: Data): Address = ???
    def updateProgram(address: Address, code: Data): Unit = ???
  }

  def exec(p: ProgramStub): Array[Data] = {
    Vm.runRaw(p.byteString, Address @@ ByteString.EMPTY, emptyState).stack.toArray
  }

  def exec(p: ProgramStub, worldState: Environment): Array[Data] = {
    Vm.runRaw(p.byteString, Address @@ ByteString.EMPTY, worldState).stack.toArray
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

  def data(i: Array[Byte]): Data =
    ByteString.copyFrom(i)

  def data(i: Byte*): Data =
    data(i.toArray)

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

    def updateProgram(address: Address, code: Data): Unit = ???

  }

}
