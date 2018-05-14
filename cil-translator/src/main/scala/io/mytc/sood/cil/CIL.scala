package io.mytc.sood.cil

import fastparse.byte.all._
import LE._
import io.mytc.sood.cil.utils._

object CIL {
  final case class CilData(stringHeap: Bytes,
                           userStringHeap: Bytes,
                           blobHeap: Bytes,
                           tableNumbers: Seq[Int],
                           tables: TablesData)

  def fromPeData(peData: PE.Info.PeData): Validated[CilData] =
    TablesData
      .fromInfo(peData)
      .map(tables => CilData(peData.stringHeap, peData.userStringHeap, peData.blobHeap, peData.tableNumbers, tables))

  type Token = TablesData.TableRowData

  sealed trait OpCode

  // prefixes
  final case class Constrained(token: Token) extends OpCode
  final case class No(byte: Byte) extends OpCode
  case object Readonly extends OpCode
  case object Tail extends OpCode
  final case class Unaligned(alignment: Byte) extends OpCode
  case object Volatile extends OpCode

  // base instructions
  case object Add extends OpCode
  case object AddOvf extends OpCode
  case object AddOvfUn extends OpCode
  case object And extends OpCode
  case object ArgList extends OpCode

  final case class Beq(target: Int) extends OpCode
  final case class BeqS(target: Byte) extends OpCode
  final case class Bge(target: Int) extends OpCode
  final case class BgeS(target: Byte) extends OpCode
  final case class BgeUn(target: Int) extends OpCode
  final case class BgeUnS(target: Byte) extends OpCode
  final case class Bgt(target: Int) extends OpCode
  final case class BgtS(target: Byte) extends OpCode
  final case class BgtUn(target: Int) extends OpCode
  final case class BgtUnS(target: Byte) extends OpCode
  final case class Ble(target: Int) extends OpCode
  final case class BleS(target: Byte) extends OpCode
  final case class BleUn(target: Int) extends OpCode
  final case class BleUnS(target: Byte) extends OpCode
  final case class Blt(target: Int) extends OpCode
  final case class BltS(target: Byte) extends OpCode
  final case class BltUn(target: Int) extends OpCode
  final case class BltUnS(target: Byte) extends OpCode
  final case class BneUn(target: Int) extends OpCode
  final case class BneUnS(target: Byte) extends OpCode
  final case class Br(target: Int) extends OpCode
  final case class BrS(target: Byte) extends OpCode

  case object Break extends OpCode

  final case class BrFalse(target: Int) extends OpCode
  final case class BrFalseS(target: Byte) extends OpCode
  final case class BrNull(target: Int) extends OpCode
  final case class BrNullS(target: Byte) extends OpCode
  final case class BrZero(target: Int) extends OpCode
  final case class BrZeroS(target: Byte) extends OpCode

  final case class BrTrue(target: Int) extends OpCode
  final case class BrTrueS(target: Byte) extends OpCode
  final case class BrInst(target: Int) extends OpCode
  final case class BrInstS(target: Byte) extends OpCode

  final case class Call(token: Token) extends OpCode
  final case class CallI(token: Token) extends OpCode

  case object Ceq extends OpCode
  case object Cgt extends OpCode
  case object CgtUn extends OpCode
  case object CkFinite extends OpCode
  case object Clt extends OpCode
  case object CltUn extends OpCode

  case object ConvI1 extends OpCode
  case object ConvI2 extends OpCode
  case object ConvI4 extends OpCode
  case object ConvI8 extends OpCode
  case object ConvR4 extends OpCode
  case object ConvR8 extends OpCode
  case object ConvU1 extends OpCode
  case object ConvU2 extends OpCode
  case object ConvU4 extends OpCode
  case object ConvU8 extends OpCode
  case object ConvI extends OpCode
  case object ConvU extends OpCode
  case object ConvRUn extends OpCode

  case object ConvOvfI1 extends OpCode
  case object ConvOvfI2 extends OpCode
  case object ConvOvfI4 extends OpCode
  case object ConvOvfI8 extends OpCode
  case object ConvOvfU1 extends OpCode
  case object ConvOvfU2 extends OpCode
  case object ConvOvfU4 extends OpCode
  case object ConvOvfU8 extends OpCode
  case object ConvOvfI extends OpCode
  case object ConvOvfU extends OpCode

  case object ConvOvfI1Un extends OpCode
  case object ConvOvfI2Un extends OpCode
  case object ConvOvfI4Un extends OpCode
  case object ConvOvfI8Un extends OpCode
  case object ConvOvfU1Un extends OpCode
  case object ConvOvfU2Un extends OpCode
  case object ConvOvfU4Un extends OpCode
  case object ConvOvfU8Un extends OpCode
  case object ConvOvfIUn extends OpCode
  case object ConvOvfUUn extends OpCode

  case object CpBlk extends OpCode

  case object Div extends OpCode
  case object DivUn extends OpCode
  case object Dup extends OpCode

  case object EndFilter extends OpCode
  case object EndFinaly extends OpCode

  case object InitBlk extends OpCode

  final case class Jmp(token: Token) extends OpCode

  final case class LdArg(num: Int) extends OpCode
  final case class LdArgS(num: Short) extends OpCode
  case object LdArg0 extends OpCode
  case object LdArg1 extends OpCode
  case object LdArg2 extends OpCode
  case object LdArg3 extends OpCode
  final case class LdArgA(num: Int) extends OpCode
  final case class LdArgAS(num: Short) extends OpCode

  final case class LdcI4(num: Int) extends OpCode
  final case class LdcI8(num: Long) extends OpCode
  final case class LdcR4(num: Float) extends OpCode
  final case class LdcR8(num: Double) extends OpCode
  case object LdcI40 extends OpCode
  case object LdcI41 extends OpCode
  case object LdcI42 extends OpCode
  case object LdcI43 extends OpCode
  case object LdcI44 extends OpCode
  case object LdcI45 extends OpCode
  case object LdcI46 extends OpCode
  case object LdcI47 extends OpCode
  case object LdcI48 extends OpCode
  case object LdcI4M1 extends OpCode
  final case class LdcI4S(num: Byte) extends OpCode

  final case class LdFtn(token: Token) extends OpCode

  case object LdIndI1 extends OpCode
  case object LdIndI2 extends OpCode
  case object LdIndI4 extends OpCode
  case object LdIndI8 extends OpCode
  case object LdIndU1 extends OpCode
  case object LdIndU2 extends OpCode
  case object LdIndU4 extends OpCode
  case object LdIndR4 extends OpCode
  case object LdIndR8 extends OpCode
  case object LdIndI extends OpCode
  case object LdIndRef extends OpCode

  final case class LdLoc(num: Int) extends OpCode
  final case class LdLocS(num: Short) extends OpCode
  case object LdLoc0 extends OpCode
  case object LdLoc1 extends OpCode
  case object LdLoc2 extends OpCode
  case object LdLoc3 extends OpCode
  final case class LdLocA(num: Int) extends OpCode
  final case class LdLocAS(num: Short) extends OpCode

  case object LdNull extends OpCode

  final case class Leave(target: Int) extends OpCode
  final case class LeaveS(target: Byte) extends OpCode

  case object LocAlloc extends OpCode

  case object Mull extends OpCode
  case object MullOvf extends OpCode
  case object MullOvfUn extends OpCode

  case object Neg extends OpCode
  case object Nop extends OpCode
  case object Not extends OpCode
  case object Or extends OpCode
  case object Pop extends OpCode
  case object Rem extends OpCode
  case object RemUn extends OpCode
  case object Ret extends OpCode
  case object Shl extends OpCode
  case object Shr extends OpCode
  case object ShrUn extends OpCode

  final case class StArg(num: Int) extends OpCode
  final case class StArgS(num: Short) extends OpCode

  case object StIndI1 extends OpCode
  case object StIndI2 extends OpCode
  case object StIndI4 extends OpCode
  case object StIndI8 extends OpCode
  case object StIndR4 extends OpCode
  case object StIndR8 extends OpCode
  case object StIndI extends OpCode
  case object StIndRef extends OpCode

  final case class StLoc(num: Int) extends OpCode
  final case class StLocS(num: Short) extends OpCode
  case object StLoc0 extends OpCode
  case object StLoc1 extends OpCode
  case object StLoc2 extends OpCode
  case object StLoc3 extends OpCode

  case object Sub extends OpCode
  case object SubOvf extends OpCode
  case object SubOvfUn extends OpCode

  final case class Switch(targets: Seq[Int]) extends OpCode

  case object Xor extends OpCode

  // object instructions

  final case class Box(typeToken: Token) extends OpCode
  final case class CallVirt(methodToken: Token) extends OpCode
  final case class CastClass(typeToken: Token) extends OpCode
  final case class CpObj(typeToken: Token) extends OpCode
  final case class InitObj(typeToken: Token) extends OpCode
  final case class IsInst(typeToken: Token) extends OpCode

  final case class LdElem(typeToken: Token) extends OpCode
  case object LdElemI1 extends OpCode
  case object LdElemI2 extends OpCode
  case object LdElemI4 extends OpCode
  case object LdElemI8 extends OpCode
  case object LdElemU1 extends OpCode
  case object LdElemU2 extends OpCode
  case object LdElemU4 extends OpCode
  case object LdElemU8 extends OpCode
  case object LdElemR4 extends OpCode
  case object LdElemR8 extends OpCode
  case object LdElemI extends OpCode
  case object LdElemRef extends OpCode
  final case class LdElemA(typeToken: Token) extends OpCode

  final case class LdFld(field: Token) extends OpCode
  final case class LdFldA(field: Token) extends OpCode
  case object LdLen extends OpCode
  final case class LdObj(typeToken: Token) extends OpCode
  final case class LdSFld(field: Token) extends OpCode
  final case class LdSFldA(field: Token) extends OpCode
  final case class LdStr(string: String) extends OpCode
  final case class LdToken(token: Token) extends OpCode
  final case class LdVirtFtn(method: Token) extends OpCode

  final case class MkRefAny(cls: Token) extends OpCode
  final case class NewArr(etype: Token) extends OpCode
  final case class NewObj(ctor: Token) extends OpCode

  case object RefAnyType extends OpCode
  final case class RefAnyVal(typeToken: Token) extends OpCode

  case object ReThrow extends OpCode
  final case class SizeOf(typeToken: Token) extends OpCode

  final case class StElem(typeToken: Token) extends OpCode
  case object StElemI1 extends OpCode
  case object StElemI2 extends OpCode
  case object StElemI4 extends OpCode
  case object StElemI8 extends OpCode
  case object StElemR4 extends OpCode
  case object StElemR8 extends OpCode
  case object StElemI extends OpCode
  case object StElemRef extends OpCode

  final case class StFld(field: Token) extends OpCode
  final case class StObj(typeToken: Token) extends OpCode
  final case class StSFld(field: Token) extends OpCode

  case object Throw extends OpCode
  final case class Unbox(valueType: Token) extends OpCode
  final case class UnboxAny(typeToken: Token) extends OpCode

  private def opCode[T](t: T): P[Validated[T]] = PassWith(validated(t))
  private def opCodeWithUInt8[T](convert: Short => T): P[Validated[T]] = P(UInt8).map(convert.andThen(validated))
  private def opCodeWithInt8[T](convert: Byte => T): P[Validated[T]] = P(Int8).map(convert.andThen(validated))
  private def opCodeWithInt32[T](convert: Int => T): P[Validated[T]] = P(Int32).map(convert.andThen(validated))
  private def opCodeWithToken[T](convert: Token => T, cilData: CilData): P[Validated[T]] = P(Int32).map(
    t => {
      val tableIdx = t >> 24
      val idx = t & 0x00ffffff
      val token = cilData.tables.tableByNum(tableIdx).map(_(idx - 1)).getOrElse(TablesData.Ignored)
      validated(convert(token))
    }
  )
  private def opCodeWithString[T](convert: String => T, cilData: CilData): P[Validated[T]] = P(Int32).map(
    t => {
      if ((t >> 24) == 0x07) {
        validationError(s"wrong token first byte: ${(t >> 24).toHexString}")
      } else {
        val idx = t & 0x00ffffff
        Heaps.userString(cilData.userStringHeap, idx.toLong).map(convert)
      }
    }
  )

  def parsers1Byte(cilData: CilData): Map[Int, P[Validated[OpCode]]] = Map(
    0x00 -> opCode(Nop),
    0x01 -> opCode(Break),
    0x02 -> opCode(LdArg0),
    0x03 -> opCode(LdArg1),
    0x04 -> opCode(LdArg2),
    0x05 -> opCode(LdArg3),
    0x06 -> opCode(LdLoc0),
    0x07 -> opCode(LdLoc1),
    0x08 -> opCode(LdLoc2),
    0x09 -> opCode(LdLoc3),
    0x0A -> opCode(StLoc0),
    0x0B -> opCode(StLoc1),
    0x0C -> opCode(StLoc2),
    0x0D -> opCode(StLoc3),
    0x0E -> opCodeWithUInt8(LdArgS),
    0x0F -> opCodeWithUInt8(LdArgAS),
    0x10 -> opCodeWithUInt8(StArgS),
    0x11 -> opCodeWithUInt8(LdLocS),
    0x12 -> opCodeWithUInt8(LdLocAS),
    0x13 -> opCodeWithUInt8(StLocS),
    0x14 -> opCode(LdNull),
    0x15 -> opCode(LdcI4M1),
    0x16 -> opCode(LdcI40),
    0x17 -> opCode(LdcI41),
    0x18 -> opCode(LdcI42),
    0x19 -> opCode(LdcI43),
    0x1A -> opCode(LdcI44),
    0x1B -> opCode(LdcI45),
    0x1C -> opCode(LdcI46),
    0x1D -> opCode(LdcI47),
    0x1E -> opCode(LdcI48),
    0x1F -> opCodeWithInt8(LdcI4S),
    0x20 -> opCodeWithInt32(LdcI4),
    0x21 -> P(Int64).map(LdcI8.andThen(validated)),
    0x22 -> P(Float32).map(LdcR4.andThen(validated)),
    0x23 -> P(Float64).map(LdcR8.andThen(validated)),
    // 0x24
    0x25 -> opCode(Dup),
    0x26 -> opCode(Pop),
    0x27 -> opCodeWithToken(Jmp, cilData),
    0x28 -> opCodeWithToken(Call, cilData),
    0x29 -> opCodeWithToken(CallI, cilData),
    0x2A -> opCode(Ret),
    0x2B -> opCodeWithInt8(BrS),
    0x2C -> opCodeWithInt8(BrFalseS),
    0x2D -> opCodeWithInt8(BrTrueS),
    0x2E -> opCodeWithInt8(BeqS),
    0x2F -> opCodeWithInt8(BgeS),
    0x30 -> opCodeWithInt8(BgtS),
    0x31 -> opCodeWithInt8(BleS),
    0x32 -> opCodeWithInt8(BltS),
    0x33 -> opCodeWithInt8(BneUnS),
    0x34 -> opCodeWithInt8(BgeUnS),
    0x35 -> opCodeWithInt8(BgtUnS),
    0x36 -> opCodeWithInt8(BleUnS),
    0x37 -> opCodeWithInt8(BltUnS),
    0x38 -> opCodeWithInt32(Br),
    0x39 -> opCodeWithInt32(BrFalse),
    0x3A -> opCodeWithInt32(BrTrue),
    0x3B -> opCodeWithInt32(Beq),
    0x3C -> opCodeWithInt32(Bge),
    0x3D -> opCodeWithInt32(Bgt),
    0x3E -> opCodeWithInt32(Ble),
    0x3F -> opCodeWithInt32(Blt),
    0x40 -> opCodeWithInt32(BneUn),
    0x41 -> opCodeWithInt32(BgeUn),
    0x42 -> opCodeWithInt32(BgtUn),
    0x43 -> opCodeWithInt32(BleUn),
    0x44 -> opCodeWithInt32(BltUn),
    0x45 -> P(UInt32).flatMap(l => Int32.rep(exactly = l.toInt).map(Switch.andThen(validated))),
    0x46 -> opCode(LdIndI1),
    0x47 -> opCode(LdIndU1),
    0x48 -> opCode(LdIndI2),
    0x49 -> opCode(LdIndU2),
    0x4A -> opCode(LdIndI4),
    0x4B -> opCode(LdIndU4),
    0x4C -> opCode(LdIndI8),
    0x4D -> opCode(LdIndI),
    0x4E -> opCode(LdIndR4),
    0x4F -> opCode(LdIndR8),
    0x50 -> opCode(LdIndRef),
    0x51 -> opCode(StIndRef),
    0x52 -> opCode(StIndI1),
    0x53 -> opCode(StIndI2),
    0x54 -> opCode(StIndI4),
    0x55 -> opCode(StIndI8),
    0x56 -> opCode(StIndR4),
    0x57 -> opCode(StIndR8),
    0x58 -> opCode(Add),
    0x59 -> opCode(Sub),
    0x5A -> opCode(Mull),
    0x5B -> opCode(Div),
    0x5C -> opCode(DivUn),
    0x5D -> opCode(Rem),
    0x5E -> opCode(RemUn),
    0x5F -> opCode(And),
    0x60 -> opCode(Or),
    0x61 -> opCode(Xor),
    0x62 -> opCode(Shl),
    0x63 -> opCode(Shr),
    0x64 -> opCode(ShrUn),
    0x65 -> opCode(Neg),
    0x66 -> opCode(Not),
    0x67 -> opCode(ConvI1),
    0x68 -> opCode(ConvI2),
    0x69 -> opCode(ConvI4),
    0x6A -> opCode(ConvI8),
    0x6B -> opCode(ConvR4),
    0x6C -> opCode(ConvR8),
    0x6D -> opCode(ConvU4),
    0x6E -> opCode(ConvU8),
    0x6F -> opCodeWithToken(CallVirt, cilData),
    0x60 -> opCode(Or),
    0x61 -> opCode(Xor),
    0x62 -> opCode(Shl),
    0x63 -> opCode(Shr),
    0x64 -> opCode(ShrUn),
    0x65 -> opCode(Neg),
    0x66 -> opCode(Not),
    0x67 -> opCode(ConvI1),
    0x68 -> opCode(ConvI2),
    0x69 -> opCode(ConvI4),
    0x6A -> opCode(ConvI8),
    0x6B -> opCode(ConvR4),
    0x6C -> opCode(ConvR8),
    0x6D -> opCode(ConvU4),
    0x6E -> opCode(ConvU8),
    0x6F -> opCodeWithToken(CallVirt, cilData),
    0x70 -> opCodeWithToken(CpObj, cilData),
    0x71 -> opCodeWithToken(LdObj, cilData),
    0x72 -> opCodeWithString(LdStr, cilData),
    0x73 -> opCodeWithToken(NewObj, cilData),
    0x74 -> opCodeWithToken(CastClass, cilData),
    0x75 -> opCodeWithToken(IsInst, cilData),
    0x76 -> opCode(ConvRUn),
    // 0x77_
    // 0x78_
    0x79 -> opCodeWithToken(Unbox, cilData),
    0x7A -> opCode(Throw),
    0x7B -> opCodeWithToken(LdFld, cilData),
    0x7C -> opCodeWithToken(LdFldA, cilData),
    0x7D -> opCodeWithToken(StFld, cilData),
    0x7E -> opCodeWithToken(LdSFld, cilData),
    0x7F -> opCodeWithToken(LdSFldA, cilData),
    0x80 -> opCodeWithToken(StSFld, cilData),
    0x81 -> opCodeWithToken(StObj, cilData),
    0x82 -> opCode(ConvOvfI1Un),
    0x83 -> opCode(ConvOvfI2Un),
    0x84 -> opCode(ConvOvfI4Un),
    0x85 -> opCode(ConvOvfI8Un),
    0x86 -> opCode(ConvOvfU1Un),
    0x87 -> opCode(ConvOvfU2Un),
    0x88 -> opCode(ConvOvfU4Un),
    0x89 -> opCode(ConvOvfU8Un),
    0x8A -> opCode(ConvOvfIUn),
    0x8B -> opCode(ConvOvfUUn),
    0x8C -> opCodeWithToken(Box, cilData),
    0x8D -> opCodeWithToken(NewArr, cilData),
    0x8E -> opCode(LdLen),
    0x8F -> opCodeWithToken(LdElemA, cilData),
    0x90 -> opCode(LdElemI1),
    0x91 -> opCode(LdElemU1),
    0x92 -> opCode(LdElemI2),
    0x93 -> opCode(LdElemU2),
    0x94 -> opCode(LdElemI4),
    0x95 -> opCode(LdElemU4),
    0x96 -> opCode(LdElemI8),
    0x97 -> opCode(LdElemI),
    0x98 -> opCode(LdElemR4),
    0x99 -> opCode(LdElemR8),
    0x9A -> opCode(LdElemRef),
    0x9B -> opCode(StElemI),
    0x9C -> opCode(StElemI1),
    0x9D -> opCode(StElemI2),
    0x9E -> opCode(StElemI4),
    0x9F -> opCode(StElemI8),
    0xA0 -> opCode(StElemR4),
    0xA1 -> opCode(StElemR8),
    0xA2 -> opCode(StElemRef),
    0xA3 -> opCodeWithToken(LdElem, cilData),
    0xA4 -> opCodeWithToken(StElem, cilData),
    0xA5 -> opCodeWithToken(UnboxAny, cilData),
    0xB3 -> opCode(ConvOvfI1),
    0xB4 -> opCode(ConvOvfU1),
    0xB5 -> opCode(ConvOvfI2),
    0xB6 -> opCode(ConvOvfU2),
    0xB7 -> opCode(ConvOvfI4),
    0xB8 -> opCode(ConvOvfU4),
    0xB9 -> opCode(ConvOvfI8),
    0xBA -> opCode(ConvOvfU8),
    0xC2 -> opCodeWithToken(RefAnyVal, cilData),
    0xC3 -> opCode(CkFinite),
    0xC6 -> opCodeWithToken(MkRefAny, cilData),
    0xD0 -> opCodeWithToken(LdToken, cilData),
    0xD1 -> opCode(ConvU2),
    0xD2 -> opCode(ConvU1),
    0xD3 -> opCode(ConvI),
    0xD4 -> opCode(ConvOvfI),
    0xD5 -> opCode(ConvOvfU),
    0xD6 -> opCode(AddOvf),
    0xD7 -> opCode(AddOvfUn),
    0xD8 -> opCode(MullOvf),
    0xD9 -> opCode(MullOvfUn),
    0xDA -> opCode(SubOvf),
    0xDB -> opCode(SubOvfUn),
    0xDC -> opCode(EndFinaly),
    0xDD -> opCodeWithInt32(Leave),
    0xDE -> opCodeWithInt8(LeaveS),
    0xDF -> opCode(StIndI),
    0xE0 -> opCode(ConvU)
  )

  def parsersAfterFE(cilData: CilData): Map[Int, P[Validated[OpCode]]] = Map(
    0x00 -> opCode(ArgList),
    0x01 -> opCode(Ceq),
    0x02 -> opCode(Cgt),
    0x03 -> opCode(CgtUn),
    0x04 -> opCode(Clt),
    0x05 -> opCode(CltUn),
    0x06 -> opCodeWithToken(LdFtn, cilData),
    0x07 -> opCodeWithToken(LdVirtFtn, cilData),
    //0x08
    0x09 -> opCodeWithInt32(LdArg),
    0x0A -> opCodeWithInt32(LdArgA),
    0x0B -> opCodeWithInt32(StArg),
    0x0C -> opCodeWithInt32(LdLoc),
    0x0D -> opCodeWithInt32(LdLocA),
    0x0E -> opCodeWithInt32(StLoc),
    0x0F -> opCode(LocAlloc),
    0x11 -> opCode(EndFilter),
    0x12 -> opCodeWithInt8(Unaligned),
    0x13 -> opCode(Volatile),
    0x14 -> opCode(Tail),
    0x15 -> opCode(LdcI4M1),
    0x16 -> opCodeWithToken(InitObj, cilData),
    0x17 -> opCodeWithToken(Constrained, cilData),
    0x18 -> opCode(CpBlk),
    0x19 -> opCode(InitBlk),
    0x1A -> opCodeWithInt8(No),
    0x1B -> opCode(ReThrow),
    0x1C -> opCodeWithToken(SizeOf, cilData),
    0x1D -> opCode(RefAnyType),
    0x1E -> opCode(Readonly)
  )

  def code(cilData: CilData): P[Validated[Seq[OpCode]]] = {
    val p1 = parsers1Byte(cilData)
    val p2 = parsersAfterFE(cilData)

    val oneByte = P(AnyByte.!).flatMap(
      b => p1(b.head & 0xff)
    )
    val twoBytes = P(BS(0xFE) ~ AnyByte.!).flatMap(
      b => p2(b.head & 0xff)
    )

    P((twoBytes | oneByte).rep).map(_.sequence)
  }
}
