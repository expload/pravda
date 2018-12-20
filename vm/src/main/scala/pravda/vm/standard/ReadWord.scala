package pravda.vm.standard

import com.google.protobuf.ByteString
import pravda.vm.Data.Array.Int8Array
import pravda.vm.Data.Type
import pravda.vm.Error.{InvalidArgument, WrongType}
import pravda.vm.WattCounter.CpuArithmetic
import pravda.vm._
import pravda.vm.operations._

object ReadWord extends FunctionDefinition {

  val id = 0x06L

  val description =
    "Takes byte array, index and size from stack. Returns size bytes from given index in the given array."

  val args: Seq[(String, Seq[Type])] = Seq(
    "size" -> Seq(Data.Type.BigInt),
    "index" -> Seq(Data.Type.BigInt),
    "array" -> Seq(Data.Type.Array)
  )

  val returns = Seq(Data.Type.Bytes)

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    val res = memory.heapGet(ref(memory.pop())) match {
      case Int8Array(data) =>
        val ind = memory.pop() match {
          case Data.Primitive.BigInt(b) =>
            if (b < 0) throw ThrowableVmError(InvalidArgument)
            else if (b > Int.MaxValue) Int.MaxValue
            else b.toInt
          case _ => throw ThrowableVmError(WrongType)
        }

        val size = memory.pop() match {
          case Data.Primitive.BigInt(b) =>
            if (b < 0) throw ThrowableVmError(InvalidArgument)
            else if (b > Int.MaxValue) Int.MaxValue
            else b.toInt
          case _ => throw ThrowableVmError(WrongType)
        }
        Data.Primitive.Bytes(ByteString.copyFrom(data.slice(ind, ind + size).toArray))
      case _ => throw ThrowableVmError(WrongType)
    }

    wattCounter.cpuUsage(CpuArithmetic)
    wattCounter.memoryUsage(res.data.size().toLong * 2)

    memory.push(res)
  }
}
