package pravda.vm.standard

import java.nio.charset.StandardCharsets

import com.google.protobuf.ByteString
import pravda.common.contrib.ripemd160
import pravda.vm._

object Ripemd160 extends FunctionDefinition {

  val id: Long = 0x02L

  val description: String =
    "Calculate RIPEMD-160 hash for message. See https://homes.esat.kuleuven.be/~bosselae/ripemd160.html"

  val returns = Seq(Data.Type.Bytes)

  val args = Seq(
    "message" -> Seq(Data.Type.Bytes, Data.Type.Utf8)
  )

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    val message = memory.pop() match {
      case Data.Primitive.Bytes(data) => data.toByteArray
      case Data.Primitive.Utf8(data)  => data.getBytes(StandardCharsets.UTF_8)
      case _                          => throw VmErrorException(VmError.WrongType)
    }
    val result = ripemd160.getHash(message)
    wattCounter.cpuUsage(message.length * WattCounter.CpuArithmetic)
    memory.push(Data.Primitive.Bytes(ByteString.copyFrom(result)))
  }
}
