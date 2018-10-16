package pravda.vm

import java.io.File

import com.google.protobuf.ByteString
import org.json4s.DefaultFormats
import pravda.common.domain.Address
import pravda.common.json._
import pravda.plaintest._
import pravda.vm
import pravda.vm.Data.Primitive
import pravda.vm.Error.DataError
import pravda.vm.asm.PravdaAssembler
import pravda.vm.asm.json._
import pravda.vm.impl.{MemoryImpl, VmImpl, WattCounterImpl}
import pravda.vm.json._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object VmSuiteData {

  final case class Preconditions(`watts-limit`: Long = 0,
                                 balances: Map[Address, Primitive.BigInt] = Map.empty,
                                 stack: Seq[Primitive] = Nil,
                                 heap: Map[Primitive.Ref, Data] = Map.empty,
                                 storage: Map[Primitive, Data] = Map.empty,
                                 programs: Map[Address, Primitive.Bytes] = Map.empty,
                                 executor: Option[Address] = None,
                                 code: List[asm.Operation])

  final case class Expectations(`watts-spent`: Long,
                                stack: Seq[Primitive] = Nil,
                                heap: Map[Primitive.Ref, Data] = Map.empty,
                                effects: Seq[vm.Effect] = Nil,
                                error: Option[vm.Error] = None)
}

import pravda.vm.VmSuiteData._

object VmSuite extends Plaintest[Preconditions, Expectations] {
  lazy val dir = new File("vm/src/test/resources")
  override lazy val ext = "sbox"
  override lazy val formats =
    DefaultFormats +
      json4sFormat[Data] +
      json4sFormat[Primitive] +
      json4sFormat[Primitive.BigInt] +
      json4sFormat[Primitive.Bytes] +
      json4sFormat[vm.Effect] +
      json4sFormat[vm.Error] +
      json4sFormat[asm.Operation] +
      json4sKeyFormat[ByteString] +
      json4sKeyFormat[Primitive.Ref] +
      json4sKeyFormat[Primitive]

  override def produce(input: Preconditions): Either[String, Expectations] = {
    val sandboxVm = new VmImpl()
    val asmProgram = PravdaAssembler.assemble(input.code, saveLabels = true)
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

    val effects = mutable.Buffer[vm.Effect]()
    val environment: Environment =
      new VmSandbox.EnvironmentSandbox(effects, input.balances.toSeq, input.programs.toSeq, pExecutor)
    val storage: Storage = new VmSandbox.StorageSandbox(effects, input.storage.toSeq)

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
              memory.currentOffset
            ))
        case ThrowableVmError(e) =>
          Some(
            RuntimeException(
              e,
              FinalState(wattCounter.spent, wattCounter.refund, wattCounter.total, memory.stack, memory.heap),
              memory.callStack,
              memory.currentOffset))
      },
      _ => None
    )

    Right(
      Expectations(
        wattCounter.spent,
        memory.stack,
        memory.heap.zipWithIndex.map { case (d, i) => Data.Primitive.Ref(i) -> d }.toMap,
        effects,
        error.map(_.error)
      )
    )
  }
}
