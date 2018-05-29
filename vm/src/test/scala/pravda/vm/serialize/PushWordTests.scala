package pravda.vm
package serialize

import VmUtils.{data, emptyState}
import Opcodes.PUSHX
import com.google.protobuf.ByteString
import utest._
import pravda.common.bytes.hex._
import pravda.common.domain.Address


object PushWordTests extends TestSuite {

  final val LEN4 = 0x04.toByte
  final val LEN3 = 0x03.toByte
  final val LEN2 = 0x02.toByte
  final val LEN1 = 0x01.toByte
  final val ZERO = 0x00.toByte

  def withLen(length: Byte, byte: Byte): Byte = {
    (length | byte).toByte
  }
  val tests = Tests {

    'pushOneByteWord - {
      val program = hex"$PUSHX 15"
      Vm.runRaw(ByteString.copyFrom(program), Address @@ ByteString.EMPTY, emptyState).stack.toArray ==> Array(data(0x15.toByte))
    }

    'pushThreeByteWord - {

      val program = hex"$PUSHX ${withLen(LEN3, 0x20.toByte)} CA AB FE 00"
      Vm.runRaw(ByteString.copyFrom(program), Address @@ ByteString.EMPTY, emptyState).stack.toArray ==> Array(
        data(hex"CA AB FE")
      )
    }

    'pushFourByteWord - {
      val program = hex"$PUSHX ${withLen(LEN4,  0x20.toByte)} CA AB FE 00"
      Vm.runRaw(ByteString.copyFrom(program), Address @@ ByteString.EMPTY, emptyState).stack.toArray ==> Array(
        data(hex"CA AB FE 00")
      )
    }
  }
}
