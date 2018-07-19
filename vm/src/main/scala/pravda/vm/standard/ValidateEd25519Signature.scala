package pravda.vm.standard
import java.nio.charset.StandardCharsets

import pravda.common.contrib.ed25519
import pravda.vm._

object ValidateEd25519Signature extends FunctionDefinition {

  val id: Long = 0x01L

  val description: String = "Validates message signed with Ed25519 algorithm. See https://ed25519.cr.yp.to"

  val returns = Seq(Data.Type.Boolean)

  val args = Seq(
    "pubKey" -> Seq(Data.Type.Bytes),
    "message" -> Seq(Data.Type.Bytes, Data.Type.Utf8),
    "signature" -> Seq(Data.Type.Bytes),
  )

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    val signature = operations.bytes(memory.pop())
    val message = memory.pop() match {
      case Data.Primitive.Bytes(data) => data.toByteArray
      case Data.Primitive.Utf8(data)  => data.getBytes(StandardCharsets.UTF_8)
      case _                          => throw VmErrorException(VmError.WrongType)
    }
    val pubKey = operations.bytes(memory.pop())
    val result = ed25519.verify(pubKey.toByteArray, message, signature.toByteArray)
    wattCounter.cpuUsage((signature.size() + message.length + pubKey.size) * WattCounter.CpuArithmetic)
    memory.push(Data.Primitive.Bool(result))
  }
}
