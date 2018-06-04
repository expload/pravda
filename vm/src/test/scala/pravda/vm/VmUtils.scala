package pravda.vm

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}
import pravda.common.bytes.byteString2hex
import pravda.vm.state._

object VmUtils {

  val emptyState: Environment = new Environment {
    def getProgram(address: Address): Option[ProgramContext] = None
    def getProgramOwner(address: Address): Option[Address] = ???
    def createProgram(owner: Address, code: Data): Address = ???
    def updateProgram(address: Address, code: Data): Data = ???
    def transfer(from: Address, to: Address, amount: NativeCoin): Unit = ???
    def balance(address: Address): NativeCoin = ???
    def withdraw(address: Address, amount: NativeCoin): Unit = ???
    def accrue(address: Address, amount: NativeCoin): Unit = ???
  }

  def stackOfExec(p: ProgramStub): Array[Data] = {
    Vm.runRaw(p.byteString, Address @@ ByteString.EMPTY, emptyState, Long.MaxValue).memory.stack.toArray
  }

  def stackOfExec(p: ProgramStub, worldState: Environment): Array[Data] = {
    Vm.runRaw(p.byteString, Address @@ ByteString.EMPTY, worldState, Long.MaxValue).memory.stack.toArray
  }

  def exec(p: ProgramStub): ExecutionResult = {
    Vm.runRaw(p.byteString, Address @@ ByteString.EMPTY, emptyState, Long.MaxValue)
  }

  def exec(p: ProgramStub, worldState: Environment): ExecutionResult = {
    Vm.runRaw(p.byteString, Address @@ ByteString.EMPTY, worldState, Long.MaxValue)
  }

  def stack(item: Data*): Array[Data] =  item.toArray

  def show(seq: Seq[Data]): String = seq.map(byteString2hex).mkString(" ")
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
    def updateProgram(address: Address, code: Data): Data = ???

    def transfer(from: Address, to: Address, amount: NativeCoin): Unit = ???
    def balance(address: Address): NativeCoin = ???
    def withdraw(address: Address, amount: NativeCoin): Unit = ???
    def accrue(address: Address, amount: NativeCoin): Unit = ???

  }

}
