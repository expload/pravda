package pravda.testkit

import java.io.File

import com.google.protobuf.ByteString
import org.json4s.DefaultFormats
import pravda.common.json._
import pravda.dotnet.DotnetCompilation
import pravda.dotnet.translation.Translator
import pravda.plaintest._
import pravda.vm
import pravda.vm.Data.Primitive
import pravda.vm._
import pravda.vm.asm.PravdaAssembler
import pravda.vm.json._

object DotnetSuiteData {
  final case class Preconditions(vm: VmSandbox.Preconditions, `dotnet-compilation`: DotnetCompilation)
}

import pravda.testkit.DotnetSuiteData._

object DotnetSuite extends Plaintest[Preconditions, VmSandbox.ExpectationsWithoutWatts] {

  lazy val dir = new File("testkit/src/test/resources")
  override lazy val ext = "sbox"
  override lazy val formats =
    DefaultFormats +
      json4sFormat[Data] +
      json4sFormat[Primitive] +
      json4sFormat[Primitive.Int64] +
      json4sFormat[Primitive.Bytes] +
      json4sFormat[vm.Effect] +
      json4sFormat[vm.Error] +
      json4sFormat[ByteString] +
      json4sKeyFormat[ByteString] +
      json4sKeyFormat[Primitive.Ref] +
      json4sKeyFormat[Primitive]

  def produce(input: Preconditions): Either[String, VmSandbox.ExpectationsWithoutWatts] = {
    import pravda.dotnet.clearPathsInPdb
    val codeE =
      for {
        files <- DotnetCompilation.run(input.`dotnet-compilation`)
        clearedFiles = clearPathsInPdb(files)
        ops <- Translator.translateAsm(clearedFiles, input.`dotnet-compilation`.`main-class`).left.map(_.mkString)
      } yield PravdaAssembler.assemble(ops, saveLabels = true)

    for {
      code <- codeE
    } yield VmSandbox.ExpectationsWithoutWatts.fromExpectations(VmSandbox.run(input.vm, code))
  }
}
