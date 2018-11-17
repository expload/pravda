package pravda.testkit

import java.io.File

import com.google.protobuf.ByteString
import org.json4s.DefaultFormats
import pravda.common.bytes
import pravda.common.domain.Address
import pravda.common.json._
import pravda.dotnet.DotnetCompilation
import pravda.dotnet.translation.Translator
import pravda.plaintest._
import pravda.vm
import pravda.vm.Data.Primitive
import pravda.vm.Error.DataError
import pravda.vm._
import pravda.vm.asm.PravdaAssembler
import pravda.vm.impl.{MemoryImpl, VmImpl, WattCounterImpl}
import pravda.vm.json._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object DotnetSuiteData {
  final case class Preconditions(balances: Map[Address, Primitive.BigInt] = Map.empty,
                                 stack: Seq[Primitive] = Nil,
                                 heap: Map[Primitive.Ref, Data] = Map.empty,
                                 storage: Map[Primitive, Data] = Map.empty,
                                 `program-storage`: Map[Address, Map[Primitive, Data]] = Map.empty,
                                 programs: Map[Address, Primitive.Bytes] = Map.empty,
                                 executor: Option[Address] = None,
                                 `dotnet-compilation`: DotnetCompilation,
                                 `app-state-info`: AppStateInfo =
                                 AppStateInfo(`app-hash` = bytes.hex2byteString("62099c6a16853f70fcf2e5a24da6e46faaf0b2541658bec668527b0436d32ece"),
                                   height = 1L))

  final case class Expectations(stack: Seq[Primitive] = Nil,
                                heap: Map[Primitive.Ref, Data] = Map.empty,
                                effects: Seq[vm.Effect] = Nil,
                                error: Option[vm.Error] = None)
}

import pravda.testkit.DotnetSuiteData._

object DotnetSuite extends Plaintest[Preconditions, Expectations] {

  lazy val dir = new File("testkit/src/test/resources")
  override lazy val ext = "sbox"
  override lazy val formats =
    DefaultFormats +
      json4sFormat[Data] +
      json4sFormat[Primitive] +
      json4sFormat[Primitive.BigInt] +
      json4sFormat[Primitive.Bytes] +
      json4sFormat[vm.Effect] +
      json4sFormat[vm.Error] +
      json4sFormat[ByteString] +
      json4sKeyFormat[ByteString] +
      json4sKeyFormat[Primitive.Ref] +
      json4sKeyFormat[Primitive]

  def produce(input: Preconditions): Either[String, Expectations] = {
    val asmE =
      for {
        files <- DotnetCompilation.run(input.`dotnet-compilation`)
        ops <- Translator.translateAsm(files, input.`dotnet-compilation`.`main-class`).left.map(_.mkString)
        asmProgram = PravdaAssembler.assemble(ops, saveLabels = true)
      } yield asmProgram

    val sandboxVm = new VmImpl()
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
    val wattCounter = new WattCounterImpl(Long.MaxValue)

    val pExecutor = input.executor.getOrElse {
      Address @@ ByteString.copyFrom((1 to 32).map(_.toByte).toArray)
    }

    val effects = mutable.Buffer[vm.Effect]()
    val environment: Environment = new VmSandbox.EnvironmentSandbox(
      effects = effects,
      initStorages = input.`program-storage`,
      initBalances = input.balances.toSeq,
      initPrograms = input.programs.toSeq,
      pExecutor = pExecutor,
      input.`app-state-info`
    )

    val storage = new VmSandbox.StorageSandbox(
      Address.Void,
      effects,
      input.storage.toSeq
    )

    for {
      asm <- asmE
    } yield {
      val error = Try {
        memory.enterProgram(Address.Void)
        sandboxVm.runBytes(
          asm.asReadOnlyByteBuffer(),
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

      Expectations(
        memory.stack,
        memory.heap.zipWithIndex.map { case (d, i) => Data.Primitive.Ref(i) -> d }.toMap,
        effects,
        error.map(_.error)
      )
    }
  }
}
