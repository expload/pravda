package pravda.common

import com.google.protobuf.ByteString

package object bytes {

  def bytes2hex(bytes: Array[Byte]): String = {
    val l = bytes.length
    if (l > 0) {
      val sb = new Array[Char](l * 2)

      def aux(i: Int, j: Int): Unit = {
        if (i < l) {
          setChar(bytes(i), j, sb)
          aux(i + 1, j + 2)
        } else {
          ()
        }
      }

      aux(0, 0)
      new String(sb)
    } else {
      ""
    }
  }

  def byteString2hex(byteString: ByteString): String = {
    val l = byteString.size()
    if (l > 0) {
      val sb = new Array[Char](l * 2)

      def aux(i: Int, j: Int): Unit = {
        if (i < l) {
          setChar(byteString.byteAt(i), j, sb)
          aux(i + 1, j + 2)
        } else {
          ()
        }
      }

      aux(0, 0)
      new String(sb)
    } else {
      ""
    }
  }

  def isHex(s: String): Boolean =
    try {
      hex2bytes(s)
      true
    } catch {
      case e: IllegalArgumentException =>
        false
    }

  def hex2byteString(hex: String): ByteString = {
    if (hex.length > 0) {
      val his = getHexInputStream
      his.setHex(hex)
      ByteString.readFrom(his)
    } else {
      ByteString.EMPTY
    }
  }

  def hex2bytes(hex: String): Array[Byte] = {
    val his = getHexInputStream
    his.setHex(hex)
    val array = new Array[Byte](hex.length / 2)

    def aux(i: Int): Unit = {
      val x = his.read()
      if (x > -1) {
        array(i) = x.toByte
        aux(i + 1)
      } else ()
    }

    aux(0)
    array
  }

  private val hexHexInputStream = new ThreadLocal[HexInputStream]()

  private def setChar(x: Byte, j: Int, array: Array[Char]): Unit = {
    val l = (x & 0xF0) >>> 4
    val r = x & 0x0F
    array(j) = from16(l)
    array(j + 1) = from16(r)
  }

  private def from16(x: Int): Char = {
    if (x < 0xA) (x + 0x30).toChar // 0-9
    else (x + 0x57).toChar
  }

  // scalafix:off DisableSyntax.keywords.null
  private def getHexInputStream: HexInputStream = {
    var his = hexHexInputStream.get()
    if (his == null) {
      his = new HexInputStream()
      hexHexInputStream.set(his)
    }
    his
  }
  // scalafix:on DisableSyntax.keywords.null

}
