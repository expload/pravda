package pravda.vm

import java.io.File

import com.google.protobuf.ByteString
import org.json4s.DefaultFormats
import pravda.common.json._
import pravda.plaintest._
import pravda.vm
import pravda.vm.Data.Primitive
import pravda.vm.asm.PravdaAssembler
import pravda.vm.json._
import pravda.vm.sandbox.VmSandbox

object VmSuiteData {
  final case class Preconditions(vm: VmSandbox.Preconditions, code: String)
}

import pravda.vm.VmSuiteData._

object VmSuite extends Plaintest[Preconditions, VmSandbox.Expectations] {
  lazy val dir = new File("vm/src/test/resources")
  override lazy val ext = "sbox"
  override lazy val formats =
    DefaultFormats +
      json4sFormat[Data] +
      json4sFormat[Primitive] +
      json4sFormat[Primitive.Int64] +
      json4sFormat[Primitive.Bytes] +
      json4sFormat[ByteString] +
      json4sFormat[vm.Effect] +
      json4sFormat[vm.Error] +
      json4sKeyFormat[ByteString] +
      json4sKeyFormat[Primitive.Ref] +
      json4sKeyFormat[Primitive]

  override def produce(input: Preconditions): Either[String, VmSandbox.Expectations] =
    for {
      asm <- PravdaAssembler.assemble(input.code, saveLabels = true).left.map(_.mkString)
    } yield VmSandbox.run(input.vm, asm)
}
