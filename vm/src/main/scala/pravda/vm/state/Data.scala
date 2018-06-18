package pravda.vm.state

object Data_ {
  final val TypeNull = 0x00.toByte
  final val TypeInt8 = 0x01.toByte
  final val TypeInt16 = 0x02.toByte
  final val TypeInt32 = 0x03.toByte
  final val TypeBigInt = 0x04.toByte
  final val TypeUint8 = 0x05.toByte
  final val TypeUint16 = 0x06.toByte
  final val TypeUint32 = 0x07.toByte
  final val TypeNumber = 0x08.toByte
  final val TypeBoolean = 0x09.toByte
  final val TypeRef = 0x0A.toByte
  final val TypeUtf8 = 0x0B.toByte
  final val TypeArray = 0x0C.toByte
  final val TypeStruct = 0x0D.toByte
}
