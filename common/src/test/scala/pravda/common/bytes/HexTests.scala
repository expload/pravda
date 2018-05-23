package pravda.common.bytes

import utest._

import hex._

object HexTests extends TestSuite {

  val tests = Tests {
    'hex - {

      val b = 0x17.toByte
      val c = 0x7a.toByte
      hex"" ==> Array.empty[Byte]
      hex"ff00${b}a3" ==> Array(0xff.toByte, 0x00.toByte, 0x17.toByte, 0xa3.toByte)
      hex"ff00a3" ==> Array(0xff.toByte, 0x00.toByte, 0xa3.toByte)
      hex"${b}" ==> Array(0x17.toByte)
      hex"${b}${c}" ==> Array(0x17.toByte, 0x7a.toByte)
      hex"db00${b}00af${c}cc" ==> Array(0xdb.toByte, 0x00.toByte, 0x17.toByte, 0x00.toByte, 0xaf.toByte, 0x7a.toByte, 0xcc.toByte)
      val arr = hex"4243"
      hex"01${arr}03" ==> Array(0x01.toByte, 0x42.toByte, 0x43.toByte, 0x03.toByte)
      hex"ff 00  a3" ==> Array(0xff.toByte, 0x00.toByte, 0xa3.toByte)
      hex"ff 00 $b a3" ==> Array(0xff.toByte, 0x00.toByte, 0x17.toByte, 0xa3.toByte)

    }
  }
}
