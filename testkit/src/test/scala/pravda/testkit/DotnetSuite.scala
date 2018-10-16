package pravda.testkit

import java.io.File
import java.nio.file.{Files, Paths}

import pravda.dotnet.parser.FileParser
import pravda.dotnet.parser.FileParser.ParsedDotnetFile
import pravda.dotnet.translation.Translator
import pravda.plaintest._
import pravda.vm._
import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import com.google.protobuf.ByteString
import org.json4s.DefaultFormats
import pravda.common.domain.Address
import pravda.common.json.json4sFormat
import pravda.vm
import pravda.vm.Data.Primitive
import pravda.vm.Error.DataError
import pravda.vm.VmSuiteData.{Expectations, Memory}
import pravda.vm.asm.PravdaAssembler
import pravda.vm.impl.{MemoryImpl, VmImpl, WattCounterImpl}
import pravda.vm.json._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object DotnetSuiteData {
  final case class Memory(stack: Seq[Primitive] = Nil, heap: Map[Primitive.Ref, Data] = Map.empty)

  final case class Preconditions(balances: Map[Address, Primitive.BigInt],
                                 memory: Memory = Memory(),
                                 storage: Map[Primitive, Data] = Map.empty,
                                 programs: Map[Address, Primitive.Bytes] = Map.empty,
                                 executor: Option[Address],
                                 dotnetCompilation: String)

  final case class Expectations(memory: Memory, effects: Seq[vm.Effect], error: Option[RuntimeException])
}

import DotnetSuiteData._

object DotnetSuite extends Plaintest[Preconditions, Expectations] {

  private def dotnetToAsm(filename: String,
                          dllsFiles: List[String],
                          mainClass: Option[String]): Either[String, List[asm.Operation]] = {
    import scala.sys.process._

    val exploadDll = new File("PravdaDotNet/Pravda.dll")

    new File("/tmp/pravda/").mkdirs()

    val tmpSrcs =
      (filename :: dllsFiles).map(f => (new File(s"dotnet-tests/resources/$f"), new File(s"/tmp/pravda/$f")))

    tmpSrcs.foreach {
      case (from, dest) =>
        if (!dest.exists()) {
          Files.copy(from.toPath, dest.toPath)
        }
    }

    val exe = File.createTempFile("dotnet-", ".exe")
    val pdb = File.createTempFile("dotnet-", ".pdb")
    s"""csc ${tmpSrcs.head._2.getAbsolutePath}
         |-out:${exe.getAbsolutePath}
         |-reference:${exploadDll.getAbsolutePath}
         |${tmpSrcs.tail.map(dll => s"-reference:${dll._2.getAbsolutePath}").mkString("\n")}
         |-debug:portable
         |-pdb:${pdb.getAbsolutePath}
      """.stripMargin.!!

    for {
      pe <- FileParser.parsePe(Files.readAllBytes(exe.toPath))
      pdb <- FileParser.parsePdb(Files.readAllBytes(pdb.toPath))
      dlls <- dllsFiles
        .map(f => FileParser.parsePe(Files.readAllBytes(Paths.get(s"dotnet-tests/resources/$f"))))
        .sequence
      asm <- Translator
        .translateAsm(ParsedDotnetFile(pe, Some(pdb)) :: dlls.map(dll => ParsedDotnetFile(dll, None)), mainClass)
        .left
        .map(_.mkString)
    } yield asm
  }

  lazy val dir = new File("testkit/src/test/resources")
  override lazy val ext = "sbox"
  override lazy val formats =
    DefaultFormats + json4sFormat[Data] + json4sFormat[vm.Effect] + json4sFormat[RuntimeException]

  def produce(input: Preconditions): Either[String, Expectations] = {
    val lines = input.dotnetCompilation.lines.toList
    val file :: dlls = lines.head.split("\\s+").toList
    val mainClass = lines.tail.headOption
    val code = dotnetToAsm(file, dlls, mainClass)
    val asmProgram = code.map(c => PravdaAssembler.assemble(c, saveLabels = true))

    val sandboxVm = new VmImpl()
    val heap = {
      if (input.memory.heap.nonEmpty) {
        val length = input.memory.heap.map(_._1.data).max + 1
        val buffer = ArrayBuffer.fill[Data](length)(Data.Primitive.Null)
        input.memory.heap.foreach { case (ref, value) => buffer(ref.data) = value }
        buffer
      } else {
        ArrayBuffer[Data]()
      }
    }
    val memory = MemoryImpl(ArrayBuffer(input.memory.stack: _*), heap)
    val wattCounter = new WattCounterImpl(Long.MaxValue)

    val pExecutor = input.executor.getOrElse {
      Address @@ ByteString.copyFrom((1 to 32).map(_.toByte).toArray)
    }

    val effects = mutable.Buffer[vm.Effect]()
    val environment: Environment =
      new VmSandbox.EnvironmentSandbox(effects, input.balances.toSeq, input.programs.toSeq, pExecutor)

    val storage: Storage = new VmSandbox.StorageSandbox(effects, input.storage.toSeq)

    for {
      a <- asmProgram
    } yield {
      val error = Try {
        memory.enterProgram(Address.Void)
        sandboxVm.runBytes(
          a.asReadOnlyByteBuffer(),
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

      Expectations(
        wattCounter.spent,
        Memory(
          memory.stack,
          memory.heap.zipWithIndex.map { case (d, i) => Data.Primitive.Ref(i) -> d }.toMap
        ),
        effects,
        error
      )
    }
  }
}
