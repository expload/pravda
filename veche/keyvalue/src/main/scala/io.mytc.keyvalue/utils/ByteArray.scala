package io.mytc.keyvalue.utils

final case class ByteArray(value: Array[Byte]) extends Ordered[ByteArray] {
  override def hashCode(): Int = java.util.Arrays.hashCode(value)
  override def equals(other: Any): Boolean = other match {
    case o: Array[Byte] => java.util.Arrays.equals(value, o)
    case o: ByteArray => java.util.Arrays.equals(value, o.value)
    case _ => false
  }

  override def compare(o: ByteArray): Int = {
    value.zip(o.value).toStream.find {
      case (b1, b2) => b1 != b2
    }.map {
      case (b1, b2) => b1.compareTo(b2)
    }.getOrElse(
      value.length.compareTo(o.value.length)
    )
  }

  def inc(): ByteArray = {
    val idx = value.lastIndexWhere(b => (b + 1).toByte != 0)
    val tail = Array.fill(value.length - (idx + 1))(0.toByte)
    if(idx < 0) {
      ByteArray(tail)
    } else {
      val head = value.take(idx)
      val upd = (value(idx) + 1).toByte
      ByteArray((head :+ upd) ++ tail)
    }
  }

}
