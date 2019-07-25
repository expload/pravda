package pravda.cli.programs

import cats.Id
import com.google.protobuf.ByteString
import pravda.cli.PravdaConfig
import pravda.node.client._
import pravda.vm.impl.MemoryImpl
import pravda.vm.{Data, ExecutionResult, FinalState}
import utest._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object RunBytecodeSuite extends TestSuite {

  final val EmptyMemory = MemoryImpl(stack = ArrayBuffer.empty, heap = ArrayBuffer.empty)

  // Programs bytecode doesn't match VM bytecode format
  // because stub VM actually doesn't execute programs.
  final val ProgramFromStdIn = ByteString.copyFrom(Array[Byte](1))
  final val ProgramFromStdInResult = Data.Primitive.Int8(42)
  final val ProgramFromStdInJson = ByteString.copyFromUtf8("""{
      |  "spentWatts" : 0,
      |  "refundWatts" : 0,
      |  "totalWatts" : 0,
      |  "stack" : [ "int8.42" ],
      |  "heap" : [ ]
      |}
      |""".stripMargin)
  final val ProgramFromFile = ByteString.copyFrom(Array[Byte](3))
  final val ProgramFromFileName = "a.out"
  final val ProgramFromFileResult = Data.Primitive.Int16(740)
  final val ProgramFromFileJson = ByteString.copyFromUtf8("""{
      |  "spentWatts" : 0,
      |  "refundWatts" : 0,
      |  "totalWatts" : 0,
      |  "stack" : [ "int16.740" ],
      |  "heap" : [ ]
      |}
      |""".stripMargin)
  final val ProgramFromFileError = ByteString.copyFromUtf8("`a.out` is not found.\n")

  private def buildExecResult(memory: MemoryImpl): ExecutionResult =
    Right(FinalState(0, 0, 0, memory.stack, memory.heap))

  val tests = Tests {
    "run using default executor and stdin" - {
      val io = new IoLanguageStub(Some(ProgramFromStdIn))
      val vm = new VmLanguage[Id] {
        def run(program: ByteString,
                executor: ByteString,
                appStateDbPath: String,
                effectsDbPath: String,
                wattLimit: Long): Id[ExecutionResult] =
          (program, executor, appStateDbPath, effectsDbPath) match {
            case (_, _, "/tmp/application-state", "/tmp/effects") =>
              buildExecResult(
                new MemoryImpl(
                  stack = ArrayBuffer(ProgramFromStdInResult),
                  heap = ArrayBuffer.empty
                )
              )
            case _ => buildExecResult(EmptyMemory)
          }
      }
      val compilers = new CompilersLanguageStub[Id]()
      val metadata = new MetadataLanguageStub[Id]()
      val ipfs = new IpfsLanguageStub[Id]()
      val program = new RunBytecode[Id](io, vm, compilers, ipfs, metadata)
      program(PravdaConfig.RunBytecode())
      assert(io.stdout.headOption.contains(ProgramFromStdInJson))
    }

    "run using default executor and file" - {
      val io = new IoLanguageStub(
        stdin = Some(ProgramFromStdIn),
        files = mutable.Map(ProgramFromFileName -> ProgramFromFile)
      )
      val vm = new VmLanguage[Id] {
        def run(program: ByteString,
                executor: ByteString,
                appStateDbPath: String,
                effectsDbPath: String,
                wattLimit: Long): Id[ExecutionResult] =
          (program, executor, appStateDbPath, effectsDbPath) match {
            case (_, _, "/tmp/application-state", "/tmp/effects") =>
              buildExecResult(
                new MemoryImpl(
                  stack = ArrayBuffer(ProgramFromFileResult),
                  heap = ArrayBuffer.empty
                )
              )
            case _ => buildExecResult(EmptyMemory)
          }
      }
      val compilers = new CompilersLanguageStub[Id]()
      val metadata = new MetadataLanguageStub[Id]()
      val ipfs = new IpfsLanguageStub[Id]()
      val program = new RunBytecode[Id](io, vm, compilers, ipfs, metadata)
      program(PravdaConfig.RunBytecode(input = Some(ProgramFromFileName)))
      assert(io.stdout.headOption.contains(ProgramFromFileJson))
    }

    "check file not found error" - {
      val io = new IoLanguageStub()
      val vm = new VmLanguage[Id] {
        def run(program: ByteString,
                executor: ByteString,
                appStateDbPath: String,
                effectsDbPath: String,
                wattLimit: Long): Id[ExecutionResult] =
          buildExecResult(EmptyMemory)
      }
      val compilers = new CompilersLanguageStub[Id]()
      val metadata = new MetadataLanguageStub[Id]()
      val ipfs = new IpfsLanguageStub[Id]()
      val program = new RunBytecode[Id](io, vm, compilers, ipfs, metadata)
      program(PravdaConfig.RunBytecode(input = Some(ProgramFromFileName)))
      assert(
        io.stderr.headOption.contains(ProgramFromFileError),
        io.exitCode == 1
      )
    }
  }

}
