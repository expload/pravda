package pravda.evm

import com.google.protobuf.ByteString
import pravda.vm.Data

package object utils {
  def evmWord(arr: Array[Byte]): Data.Primitive.Bytes =
    Data.Primitive.Bytes(ByteString.copyFrom(arr).concat(ByteString.copyFrom(Array.fill[Byte](32 - arr.length)(0))))
}
