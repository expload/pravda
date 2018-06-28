package pravda.cli.programs

import cats.Id
import com.google.protobuf.ByteString
import pravda.cli.PravdaConfig
import pravda.cli.languages._
import pravda.vm.state.VmMemory
import utest._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object RunBytecodeSuite extends TestSuite {

  final val EmptyMemory = VmMemory(stack = ArrayBuffer.empty, heap = ArrayBuffer.empty)

  // Programs bytecode doesn't match VM bytecode format
  // because stub VM actually doesn't execute programs.
  final val ProgramFromStdIn = ByteString.copyFrom(Array[Byte](1))
  final val ProgramFromStdInResult = ByteString.copyFrom(Array[Byte](2))
  final val ProgramFromStdInJson = ByteString.copyFromUtf8("""{"stack":["02"],"heap":[]}""")
  final val ProgramFromFile = ByteString.copyFrom(Array[Byte](3))
  final val ProgramFromFileName = "a.out"
  final val ProgramFromFileResult = ByteString.copyFrom(Array[Byte](4))
  final val ProgramFromFileJson = ByteString.copyFromUtf8("""{"stack":["04"],"heap":[]}""")
  final val ProgramFromFileError = ByteString.copyFromUtf8("`a.out` is not found.\n")

  val tests = Tests {
    "run using default executor and stdin" - {
      val io = new IoLanguageStub(Some(ProgramFromStdIn))
      val vm = new VmLanguage[Id] {
        def run(program: ByteString, executor: ByteString, storagePath: String, wattLimit: Long): Id[Either[String, VmMemory]] =
          (program, executor, storagePath) match {
            case (_, _, "/tmp/") =>
              Right(
                VmMemory(
                  stack = ArrayBuffer(ProgramFromStdInResult),
                  heap = ArrayBuffer.empty
                )
              )
            case _ => Right(EmptyMemory)
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
        def run(program: ByteString, executor: ByteString, storagePath: String, wattLimit: Long): Id[Either[String, VmMemory]] =
          (program, executor, storagePath) match {
            case (_, _, "/tmp/") =>
              Right(
                VmMemory(
                  stack = ArrayBuffer(ProgramFromFileResult),
                  heap = ArrayBuffer.empty
                )
              )
            case _ => Right(EmptyMemory)
          }
      }
      val program = new RunBytecode[Id](io, vm)
      program(PravdaConfig.RunBytecode(input = Some(ProgramFromFileName)))
      assert(io.stdout.headOption.contains(ProgramFromFileJson))
    }

    "check file not found error" - {
      val io = new IoLanguageStub()
      val vm = new VmLanguage[Id] {
        def run(program: ByteString, executor: ByteString, storagePath: String, wattLimit: Long): Id[Either[String, VmMemory]] =
          Right(EmptyMemory)
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
