package io.mytc.timechain

import java.io.InputStream

import com.google.protobuf.ByteString

import scala.concurrent.Future

package object utils {

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
      }
      else ()
    }
    aux(0)
    array
  }

  def bytes2hex(byteString: ByteString): String = {
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

  def detectCpuArchitecture(): Future[CpuArchitecture] = Future.successful {
    System.getProperty("os.arch").toLowerCase match {
      case s if s.contains("amd64") => CpuArchitecture.x86_64
      case s if s.contains("x86_64") => CpuArchitecture.x86_64
      case s if s.contains("x86") => CpuArchitecture.x86
      case _ => CpuArchitecture.Unsupported
    }
  }

  def detectOperationSystem(): Future[OperationSystem] = Future.successful {
    println(s"System.getProperty(os.name)=${System.getProperty("os.name")}")
    System.getProperty("os.name").toLowerCase match {
      case s if s.contains("mac") => OperationSystem.MacOS
      case s if s.contains("linux") => OperationSystem.Linux
      case s if s.contains("win") => OperationSystem.Windows
      case _ => OperationSystem.Unsupported
    }
  }

  private final val NotHex = "This is not hex string"

  private val hexHexInputStream = new ThreadLocal[HexInputStream]()

  private def setChar(x: Int, j: Int, array: Array[Char]): Unit = {
    val l = (x & 0xF0) >>> 4
    val r = x & 0x0F
    array(j) = from16(l)
    array(j + 1) = from16(r)
  }

  private def from16(x: Int): Char = {
    if (x < 0xA) (x + 0x30).toChar // 0-9
    else (x + 0x57).toChar
  }

  private def getHexInputStream: HexInputStream = {
    var his = hexHexInputStream.get()
    if (his == null) {
      his = new HexInputStream()
      hexHexInputStream.set(his)
    }
    his
  }

  private final class HexInputStream extends InputStream {

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

}
