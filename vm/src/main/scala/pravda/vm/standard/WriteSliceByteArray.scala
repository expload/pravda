package pravda.vm.standard

import pravda.vm.Data.Array.Int8Array
import pravda.vm.Data.Type
import pravda.vm.Error.{InvalidArgument, WrongType}
import pravda.vm.WattCounter.CpuArithmetic
import pravda.vm._
import pravda.vm.operations._

object WriteSliceByteArray extends FunctionDefinition {

  val id = 0x07L

  val description =
    "Takes byte array, index from stack, bytes to write. " +
      "Writes given bytes from given index in the given array. " +
      "Returns reference to array"

  val args: Seq[(String, Seq[Type])] = Seq(
    "bytes" -> Seq(Data.Type.Bytes),
    "index" -> Seq(Data.Type.BigInt),
    "array" -> Seq(Data.Type.Array)
  )

  val returns = Seq(Data.Type.Array)

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    val r = ref(memory.pop())
    val res = memory.heapGet(r) match {
      case Int8Array(data) =>
        val ind = memory.pop() match {
          case Data.Primitive.BigInt(b) =>
            if (b < 0) throw ThrowableVmError(InvalidArgument)
            else if (b > data.length) throw ThrowableVmError(InvalidArgument)
            else b.toInt
          case _ => throw ThrowableVmError(WrongType)
        }

        val bytes = memory.pop() match {
          case Data.Primitive.Bytes(b) =>
            if (ind + b.size() > data.length) throw ThrowableVmError(InvalidArgument)
            else b
          case _ => throw ThrowableVmError(WrongType)
        }

        bytes.toByteArray.zipWithIndex.foreach {
          case (b, i) => data(ind + i) = b
        }

        wattCounter.cpuUsage(bytes.size() * CpuArithmetic)

        r

      case _ => throw ThrowableVmError(WrongType)
    }

    memory.push(res)
  }
}
