package io.mytc.sood.vm
package serialize

import VmUtils.{binaryData, emptyState}
import Opcodes.int.PUSHX
import com.google.protobuf.ByteString
import utest._

object PushWordTests extends TestSuite {

  final val LEN4 = 0x04
  final val LEN3 = 0x03
  final val LEN2 = 0x02
  final val LEN1 = 0x01
  final val ZERO = 0x00


  val tests = Tests {
    'pushOneByteWord - {
      val program = bytes(PUSHX, 0x15)
      Vm.runRaw(ByteString.copyFrom(program), ByteString.EMPTY, emptyState).stack.toArray ==> Array(binaryData(0x15))
    }

    'pushThreeByteWord - {
      val program = bytes(PUSHX, LEN3 | 0x20, 0xCA, 0xAB, 0xFE, 0x00)
      Vm.runRaw(ByteString.copyFrom(program), ByteString.EMPTY, emptyState).stack.toArray ==> Array(
        binaryData(0xCA, 0xAB, 0xFE)
      )
    }

    'pushFourByteWord - {
      val program = bytes(PUSHX, LEN4 | 0x20, 0xCA, 0xAB, 0xFE, 0x00)
      Vm.runRaw(ByteString.copyFrom(program), ByteString.EMPTY, emptyState).stack.toArray ==> Array(
        binaryData(0xCA, 0xAB, 0xFE, 0x00)
      )
    }
  }
}
