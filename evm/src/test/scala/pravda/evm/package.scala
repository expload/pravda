package pravda

import fastparse.byte.all._
import pravda.vm.VmSandbox._
import pravda.vm.{Data, VmSandbox}
import pravda.vm.asm.Operation

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

package object evm {

  def run(either: Either[String, Seq[Operation]], pre: Preconditions): Either[String, Expectations] =
    either.map(ops => VmSandbox.sandboxRun(ops, pre))

  def expectations(watts: Long = 0L,
                   stack: Seq[Data.Primitive] = Nil,
                   heap: Map[Data.Primitive.Ref, Data] = Map.empty,
                   effects: Seq[EnvironmentEffect] = ArrayBuffer.empty,
                   events: Seq[EnviromentEvent] = ArrayBuffer.empty,
                   error: Option[String] = None): Expectations =
    Expectations(watts, Memory(stack, heap), effects, events, error)

  def readSolidityBinFile(filename: String): Bytes = {

    val s = Source.fromResource(filename).mkString
    val allBytes = Bytes(s.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte))
    allBytes.dropRight(43)
    // for dropRight(43) see https://ethereum.stackexchange.com/questions/42584/what-is-auxdata-in-the-asm-output-from-solc
    // we just drop auxdata
  }
}
