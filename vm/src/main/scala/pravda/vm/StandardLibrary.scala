package pravda.vm

import com.google.protobuf.ByteString
import pravda.common.contrib.{ed25519, ripemd160}

object StandardLibrary {

  /** (pubKey: Bytes, message: Bytes, signature: Bytes) => Bool */
  final val ValidateEd25519Signature = 0x01L

  /** (message: Bytes) => Bool */
  final val CalculateRipemd160Hash = 0x02L

  val implementation: Map[Long, (Memory, WattCounter) => Unit] = Map(
    ValidateEd25519Signature -> { (memory, wattCounter) =>
      val signature = operations.bytes(memory.pop())
      val message = operations.bytes(memory.pop())
      val pubKey = operations.bytes(memory.pop())
      val result = ed25519.verify(pubKey.toByteArray, message.toByteArray, signature.toByteArray)
      wattCounter.cpuUsage((signature.size() + message.size() + pubKey.size) * WattCounter.CpuArithmetic)
      memory.push(Data.Primitive.Bool(result))
    },
    CalculateRipemd160Hash -> { (memory, wattCounter) =>
      val message = operations.bytes(memory.pop())
      val result = ripemd160.getHash(message.toByteArray)
      wattCounter.cpuUsage(message.size() * WattCounter.CpuArithmetic)
      memory.push(Data.Primitive.Bytes(ByteString.copyFrom(result)))
    }
  )
}
