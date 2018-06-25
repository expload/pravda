package pravda.vm

import java.nio.ByteBuffer
import Data.Primitive._

import scala.annotation.switch

sealed trait Meta {

  import Meta._

  def writeToByteBuffer(buffer: ByteBuffer): Unit = this match {
    case LabelDef(name) =>
      buffer.put(TypeLabelDef.toByte)
      Data.Primitive.Utf8(name).writeToByteBuffer(buffer)
    case LabelUse(name) =>
      buffer.put(TypeLabelUse.toByte)
      Utf8(name).writeToByteBuffer(buffer)
    case Custom(name) =>
      buffer.put(TypeCustom.toByte)
      Utf8(name).writeToByteBuffer(buffer)
  }
}

object Meta {

  final case class LabelDef(name: String) extends Meta
  final case class LabelUse(name: String) extends Meta
  final case class Custom(name: String)   extends Meta

  def readFromByteBuffer(buffer: ByteBuffer): Meta = {

    def readString() = Data.readFromByteBuffer(buffer) match {
      case Utf8(data) => data
      case value      => throw Data.TypeUnexpectedException(value.getClass, buffer.position)
    }

    (buffer.get & 0xFF: @switch) match {
      case TypeLabelDef => LabelDef(readString())
      case TypeLabelUse => LabelUse(readString())
      case TypeCustom   => Custom(readString())
    }
  }

  final val TypeLabelDef = 0x00
  final val TypeLabelUse = 0x01
  final val TypeCustom = 0xFF
}
