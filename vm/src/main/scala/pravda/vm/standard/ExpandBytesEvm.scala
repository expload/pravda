package pravda.vm.standard

import com.google.protobuf.ByteString
import pravda.vm.Data.Type
import pravda.vm.Error.InvalidArgument
import pravda.vm.WattCounter.CpuArithmetic
import pravda.vm._
import pravda.vm.operations._

object ExpandBytesEvm extends FunctionDefinition {

  val id = 0x09L

  val description =
    "Takes bytes from stack. Return expanded to 32 length bytes. "

  val args: Seq[(String, Seq[Type])] = Seq(
    "bytes" -> Seq(Data.Type.Bytes)
  )

  val returns = Seq(Data.Type.Bytes)

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    val bs = bytes(memory.pop())
    val res = if (bs.size() > 32) {
      throw ThrowableVmError(InvalidArgument)
    } else {
      Data.Primitive.Bytes(bs.concat(ByteString.copyFrom(Array.fill[Byte](32 - bs.size())(0))))
    }

    wattCounter.cpuUsage(CpuArithmetic)
    wattCounter.memoryUsage(32L)

    memory.push(res)
  }
}
