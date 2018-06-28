package pravda.cli.programs

import cats.Id
import com.google.protobuf.ByteString
import pravda.cli.PravdaConfig
import pravda.cli.languages._
import pravda.vm.impl.{MemoryImpl, WattCounterImpl}
import pravda.vm.{Data, ExecutionResult}
import utest._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object RunBytecodeSuite extends TestSuite {

  final val EmptyMemory = MemoryImpl(stack = ArrayBuffer.empty, heap = ArrayBuffer.empty)

  // Programs bytecode doesn't match VM bytecode format
  // because stub VM actually doesn't execute programs.
  final val ProgramFromStdIn = ByteString.copyFrom(Array[Byte](1))
  final val ProgramFromStdInResult = Data.Primitive.Int8(42)
  final val ProgramFromStdInJson = ByteString.copyFromUtf8(
    """{
      |  "spentWatts" : 0,
      |  "refundWatts" : 0,
      |  "totalWatts" : 0,
      |  "stack" : [ "int8(42)" ],
      |  "heap" : [ ]
      |}""".stripMargin)
  final val ProgramFromFile = ByteString.copyFrom(Array[Byte](3))
  final val ProgramFromFileName = "a.out"
  final val ProgramFromFileResult = Data.Primitive.Int16(740)
  final val ProgramFromFileJson = ByteString.copyFromUtf8(
    """{
      |  "spentWatts" : 0,
      |  "refundWatts" : 0,
      |  "totalWatts" : 0,
      |  "stack" : [ "int16(740)" ],
      |  "heap" : [ ]
      |}""".stripMargin)
  final val ProgramFromFileError = ByteString.copyFromUtf8("`a.out` is not found.\n")

  private def buildExecResult(memory: MemoryImpl): Either[String, ExecutionResult] = Right {
    ExecutionResult(memory, None, new WattCounterImpl(0))
  }

  val tests = Tests {
    "run using default executor and stdin" - {
      val io = new IoLanguageStub(Some(ProgramFromStdIn))
      val vm = new VmLanguage[Id] {
        def run(program: ByteString, executor: ByteString, storagePath: String, wattLimit: Long): Id[Either[String, ExecutionResult]] =
          (program, executor, storagePath) match {
            case (_, _, "/tmp/") =>
                buildExecResult(
                  new MemoryImpl(
                    stack = ArrayBuffer(ProgramFromStdInResult),
                    heap = ArrayBuffer.empty
                  )
                )
            case _ => buildExecResult(EmptyMemory)
          }
      }
      val program = new RunBytecode[Id](io, vm)
      program(PravdaConfig.RunBytecode())
      assert(io.stdout.headOption.contains(ProgramFromStdInJson))
    }

    "run using default executor and file" - {
      val io = new IoLanguageStub(
        stdin = Some(ProgramFromStdIn),
        files = mutable.Map(ProgramFromFileName -> ProgramFromFile)
      )
      val vm = new VmLanguage[Id] {
        def run(program: ByteString, executor: ByteString, storagePath: String, wattLimit: Long): Id[Either[String, ExecutionResult]] =
          (program, executor, storagePath) match {
            case (_, _, "/tmp/") =>
              buildExecResult(
                new MemoryImpl(
                  stack = ArrayBuffer(ProgramFromFileResult),
                  heap = ArrayBuffer.empty
                )
              )
            case _ => buildExecResult(EmptyMemory)
          }
      }
      val program = new RunBytecode[Id](io, vm)
      program(PravdaConfig.RunBytecode(input = Some(ProgramFromFileName)))
      assert(io.stdout.headOption.contains(ProgramFromFileJson))
    }

    "check file not found error" - {
      val io = new IoLanguageStub()
      val vm = new VmLanguage[Id] {
        def run(program: ByteString, executor: ByteString, storagePath: String, wattLimit: Long): Id[Either[String, ExecutionResult]] =
          buildExecResult(EmptyMemory)
      }
      val program = new RunBytecode[Id](io, vm)
      program(PravdaConfig.RunBytecode(input = Some(ProgramFromFileName)))
      assert(
        io.stderr.headOption.contains(ProgramFromFileError),
        io.exitCode == 1
      )
    }
  }

}
