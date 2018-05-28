package pravda.common

import java.nio.charset.{Charset, StandardCharsets}

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

  def isHex(s: String): Boolean = {
    (s.length % 2 == 0) && s.forall(x => HexInputStream.to16(x) > -1)
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

  def longToBytes(l: Long): Array[Byte] = {
    val array = new Array[Byte](8)
    array(0) = (l >> 56).toByte
    array(1) = (l >> 48 & 0xff).toByte
    array(2) = (l >> 40 & 0xff).toByte
    array(3) = (l >> 32 & 0xff).toByte
    array(4) = (l >> 24 & 0xff).toByte
    array(5) = (l >> 16 & 0xff).toByte
    array(6) = (l >> 8 & 0xff).toByte
    array(7) = (l & 0xff).toByte
    array
  }

  def bytesToLong(array: Array[Byte]): Long = {
    var l = 0L
    array.foreach { b =>
      l = (l << 8) | b
    }
    l
  }

  def intToBytes(l: Int): Array[Byte] = {
    val array = new Array[Byte](4)
    array(0) = (l >> 24).toByte
    array(1) = (l >> 16 & 0xff).toByte
    array(2) = (l >> 8 & 0xff).toByte
    array(3) = (l & 0xff).toByte
    array
  }

  def bytesToInt(array: Array[Byte]): Int = {
    var i = 0
    array.foreach { b =>
      i = (i << 8) | b
    }
    i
  }

  def stringToBytes(str: String, charset: Charset = StandardCharsets.UTF_8): Array[Byte] = {
    str.getBytes(charset)
  }

  def bytesToString(array: Array[Byte], charset: Charset = StandardCharsets.UTF_8): String = {
    new String(array, charset)
  }

}
