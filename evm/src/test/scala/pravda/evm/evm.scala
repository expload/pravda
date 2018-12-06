package pravda.evm

import java.io.{File, FileOutputStream}

import com.google.protobuf.ByteString
import fastparse.byte.all._
import pravda.common.domain.Address
import pravda.evm.abi.parse.AbiParser.{AbiConstructor, AbiEvent, AbiFunction, AbiObject, Argument}
import pravda.vm.Data.Primitive
import pravda.vm.Error.DataError
import pravda.vm.VmSuiteData.Expectations
import pravda.vm._
import pravda.vm.asm.{Operation, PravdaAssembler}
import pravda.vm.impl._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.Try

final case class Preconditions(`watts-limit`: Long = 0,
                               balances: Map[Address, Primitive.BigInt] = Map.empty,
                               stack: Seq[Primitive] = Nil,
                               heap: Map[Primitive.Ref, Data] = Map.empty,
                               storage: Map[Primitive, Data] = Map.empty,
                               `program-storage`: Map[Address, Map[Primitive, Data]] = Map.empty,
                               programs: Map[Address, Primitive.Bytes] = Map.empty,
                               executor: Option[Address] = None)
package object evm {

  def run(opsE: Either[String, Seq[Operation]], input: Preconditions): Either[String, Expectations] = {
    opsE.map { ops =>
      val sandboxVm = new VmImpl()
      val asmProgram = PravdaAssembler.assemble(ops, saveLabels = true)
      val heap = {
        if (input.heap.nonEmpty) {
          val length = input.heap.map(_._1.data).max + 1
          val buffer = ArrayBuffer.fill[Data](length)(Data.Primitive.Null)
          input.heap.foreach { case (ref, value) => buffer(ref.data) = value }
          buffer
        } else {
          ArrayBuffer[Data]()
        }
      }
      val memory = MemoryImpl(ArrayBuffer(input.stack: _*), heap)
      val wattCounter = new WattCounterImpl(input.`watts-limit`)

      val pExecutor = input.executor.getOrElse {
        Address @@ ByteString.copyFrom((1 to 32).map(_.toByte).toArray)
      }

      val effects = mutable.Buffer[pravda.vm.Effect]()
      val environment: Environment = new VmSandbox.EnvironmentSandbox(
        effects,
        input.`program-storage`,
        input.balances.toSeq,
        input.programs.toSeq,
        pExecutor
      )
      val storage = new VmSandbox.StorageSandbox(Address.Void, effects, input.storage.toSeq)

      val error = Try {
        memory.enterProgram(Address.Void)
        sandboxVm.runBytes(
          asmProgram.asReadOnlyByteBuffer(),
          environment,
          memory,
          wattCounter,
          Some(storage),
          Some(Address.Void),
          pcallAllowed = true
        )
        memory.exitProgram()
      }.fold(
        {
          case e: Data.DataException =>
            Some(
              RuntimeException(
                DataError(e.getMessage),
                FinalState(wattCounter.spent, wattCounter.refund, wattCounter.total, memory.stack, memory.heap),
                memory.callStack,
                memory.currentCounter
              ))
          case ThrowableVmError(e) =>
            Some(
              RuntimeException(
                e,
                FinalState(wattCounter.spent, wattCounter.refund, wattCounter.total, memory.stack, memory.heap),
                memory.callStack,
                memory.currentCounter))
        },
        _ => None
      )

      Expectations(wattCounter.spent, memory.stack, memory.heap.zipWithIndex.map {
        case (d, i) => Data.Primitive.Ref(i) -> d
      }.toMap, effects, error.map(_.error))

    }
  }

  def expectations(watts: Long = 0L,
                   stack: Seq[Data.Primitive] = Nil,
                   heap: Map[Data.Primitive.Ref, Data] = Map.empty,
                   effects: Seq[pravda.vm.Effect] = ArrayBuffer.empty,
                   error: Option[pravda.vm.Error] = None): Expectations =
    Expectations(watts, stack, heap, effects, error)

  def readSolidityBinFile(filename: String): Bytes = {

    val s = Source.fromResource(filename).mkString
    val allBytes = Bytes(s.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte))
    allBytes.dropRight(43)
    // for dropRight(43) see https://ethereum.stackexchange.com/questions/42584/what-is-auxdata-in-the-asm-output-from-solc
    // we just drop auxdata
  }

  def readSolidityBinFile(file: File): Bytes = {

    val s = Source.fromFile(file).mkString
    val allBytes = Bytes(s.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte))
    allBytes.dropRight(43)
    // for dropRight(43) see https://ethereum.stackexchange.com/questions/42584/what-is-auxdata-in-the-asm-output-from-solc
    // we just drop auxdata
  }

  def writeSolidityBinFile(filename: String): Bytes = {

    val s = Source.fromResource(filename).mkString
    val allBytes = Bytes(s.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte))

    var out = None: Option[FileOutputStream]
    try {
      out = Some(new FileOutputStream("bla"))
      allBytes.toArray.foreach(c => out.get.write(c.toInt))

    } finally {
      println("entered finally ...")
      if (out.isDefined) out.get.close
    }

    allBytes
  }

  def readSolidityABI(filename: String): String = Source.fromResource(filename).mkString

  def printSeq(vars: Seq[Argument]): Seq[String] = {
    vars.map(
      { case Argument(name, t, indexed) => s"""Variable("$name","$t",$indexed)""" }
    )
  }

  def printOpt(vars: Option[String]): String = {
    vars match {
      case Some(s) => s"""Option("$s")"""
      case None    => "None"
    }
  }

  def printToTest(fs: List[AbiObject]): List[String] = {
    fs.map {
      case AbiFunction(const, name, in, out, payable, statemut, newName) =>
        val input = s"Vector(${printSeq(in).mkString(",")})"
        val output = s"Vector(${printSeq(out).mkString(",")})"
        val newNam = printOpt(newName)
        s"""ABIFunction($const,"$name",$input,$output,$payable,"$statemut",$newNam)"""
      case AbiEvent(name, in, anon) =>
        val input = s"Vector(${printSeq(in).mkString(",")})"
        s"""ABIEvent("$name",$input,$anon)"""

      case AbiConstructor(in, pay, stat) =>
        val input = s"Vector(${printSeq(in).mkString(",")})"
        s"""ABIConstructor($input,$pay,"$stat")"""

    }
  }
}
