package io.mytc.timechain.utils

import java.io.InputStream

final class HexInputStream extends InputStream {

  import HexInputStream._
  
  private var i = 0
  private var hex: CharSequence = ""

  private def to16(code: Char): Int = {
    if (code > 0x2F && code < 0x3A) code - 0x30 // 0-9
    else if (code > 0x40 && code < 0x47) code - 0x41 + 10 // A-F
    else if (code > 0x60 && code < 0x67) code - 0x61 + 10 // a-f
    else throw new IllegalArgumentException(NotHex)
  }

  def setHex(hex: CharSequence): Unit = {
    if (hex.length() % 2 != 0)
      throw new IllegalArgumentException(NotHex)
    this.hex = hex
    this.i = 0
  }

  def read(): Int = {
    if (i < hex.length) {
      val l = to16(hex.charAt(i))
      val r = to16(hex.charAt(i + 1))
      i += 2
      l << 4 | r
    } else {
      -1
    }
  }
}

object HexInputStream {
  final val NotHex = "This is not hex string"
}
