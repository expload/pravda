package io.mytc.sood.cil

import fastparse.byte.all._
import LE._

object CIL {
  type Token = Int

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

  final case class Switch(targets: Seq[Int])

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
  final case class LdStr(string: Token) extends OpCode
  final case class LdToken(token: Token) extends OpCode
  final case class LdVirtFtn(method: Token) extends OpCode

  final case class MkRefAny(cls: Token) extends OpCode
  final case class NewArr(etype: Token) extends OpCode
  final case class NewObj(ctor: Token) extends OpCode

  case object RefAnyType extends OpCode
  case object RefAnyVal extends OpCode

  case object ReThrow extends OpCode
  case object SizeOf extends OpCode

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

  private def opCodeWithUInt8[T](convert: Short => T): P[T] = P(UInt8).map(convert)
  private def opCodeWithInt8[T](convert: Byte => T): P[T] = P(Int8).map(convert)
  private def opCodeWithInt32[T](convert: Int => T): P[T] = P(Int32).map(convert)
  private def opCodeWithToken[T](convert: Token => T): P[T] = P(Int32).map(convert)

  val parsers1Byte: Map[Int, P[OpCode]] = Map(
    0x00 -> PassWith(Nop),
    0x01 -> PassWith(Break),
    0x02 -> PassWith(LdArg0),
    0x03 -> PassWith(LdArg1),
    0x04 -> PassWith(LdArg2),
    0x05 -> PassWith(LdArg3),
    0x06 -> PassWith(LdLoc0),
    0x07 -> PassWith(LdLoc1),
    0x08 -> PassWith(LdLoc2),
    0x09 -> PassWith(LdLoc3),
    0x0A -> PassWith(StLoc0),
    0x0B -> PassWith(StLoc1),
    0x0C -> PassWith(StLoc2),
    0x0D -> PassWith(StLoc3),
    0x0E -> opCodeWithUInt8(LdArgS),
    0x0F -> opCodeWithUInt8(LdArgAS),

    0x10 -> opCodeWithUInt8(StArgS),
    0x11 -> opCodeWithUInt8(LdLocS),
    0x12 -> opCodeWithUInt8(LdLocAS),
    0x13 -> opCodeWithUInt8(StLocS),
    0x14 -> PassWith(LdNull),
    0x15 -> PassWith(LdcI4M1),
    0x16 -> PassWith(LdcI40),
    0x17 -> PassWith(LdcI41),
    0x18 -> PassWith(LdcI42),
    0x19 -> PassWith(LdcI43),
    0x1A -> PassWith(LdcI44),
    0x1B -> PassWith(LdcI45),
    0x1C -> PassWith(LdcI46),
    0x1D -> PassWith(LdcI47),
    0x1E -> PassWith(LdcI48),
    0x1F -> opCodeWithInt8(LdcI4S),

    0x20 -> opCodeWithInt32(LdcI4),
    0x21 -> P(Int64).map(LdcI8),
    0x22 -> P(Float32).map(LdcR4),
    0x23 -> P(Float64).map(LdcR8),
    // 0x24
    0x26 -> PassWith(Pop),
    0x27 -> opCodeWithToken(Jmp),
    0x28 -> opCodeWithToken(Call),
    0x29 -> opCodeWithToken(CallI),
    0x2A -> PassWith(Ret),
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
    0x45 -> /*switch*/PassWith(Nop), // FIXME !!!
    0x46 -> PassWith(LdIndI1),
    0x47 -> PassWith(LdIndU1),
    0x48 -> PassWith(LdIndI2),
    0x49 -> PassWith(LdIndU2),
    0x4A -> PassWith(LdIndI4),
    0x4B -> PassWith(LdIndU4),
    0x4C -> PassWith(LdIndI8),
    0x4D -> PassWith(LdIndI),
    0x4E -> PassWith(LdIndR4),
    0x4F -> PassWith(LdIndR8),

    0x50 -> PassWith(LdIndRef),
    0x51 -> PassWith(StIndRef),
    0x52 -> PassWith(StIndI1),
    0x53 -> PassWith(StIndI2),
    0x54 -> PassWith(StIndI4),
    0x55 -> PassWith(StIndI8),
    0x56 -> PassWith(StIndR4),
    0x57 -> PassWith(StIndR8),
    0x58 -> PassWith(Add),
    0x59 -> PassWith(Sub),
    0x5A -> PassWith(Mull),
    0x5B -> PassWith(Div),
    0x5C -> PassWith(DivUn),
    0x5D -> PassWith(Rem),
    0x5E -> PassWith(RemUn),
    0x5F -> PassWith(And),

    0x60 -> PassWith(Or),
    0x61 -> PassWith(Xor),
    0x62 -> PassWith(Shl),
    0x63 -> PassWith(Shr),
    0x64 -> PassWith(ShrUn),
    0x65 -> PassWith(Neg),
    0x66 -> PassWith(Not),
    0x67 -> PassWith(ConvI1),
    0x68 -> PassWith(ConvI2),
    0x69 -> PassWith(ConvI4),
    0x6A -> PassWith(ConvI8),
    0x6B -> PassWith(ConvR4),
    0x6C -> PassWith(ConvR8),
    0x6D -> PassWith(ConvU4),
    0x6E -> PassWith(ConvU8),
    0x6F -> opCodeWithToken(CallVirt),

    0x60 -> PassWith(Or),
    0x61 -> PassWith(Xor),
    0x62 -> PassWith(Shl),
    0x63 -> PassWith(Shr),
    0x64 -> PassWith(ShrUn),
    0x65 -> PassWith(Neg),
    0x66 -> PassWith(Not),
    0x67 -> PassWith(ConvI1),
    0x68 -> PassWith(ConvI2),
    0x69 -> PassWith(ConvI4),
    0x6A -> PassWith(ConvI8),
    0x6B -> PassWith(ConvR4),
    0x6C -> PassWith(ConvR8),
    0x6D -> PassWith(ConvU4),
    0x6E -> PassWith(ConvU8),
    0x6F -> opCodeWithToken(CallVirt),

    0x70 -> opCodeWithToken(CpObj),
    0x71 -> opCodeWithToken(LdObj),
    0x72 -> opCodeWithToken(LdStr),
    0x73 -> opCodeWithToken(NewObj),
    0x74 -> opCodeWithToken(CastClass),
    0x75 -> opCodeWithToken(IsInst),
    0x76 -> PassWith(ConvRUn),
    // 0x77
    // 0x78
    0x79 -> opCodeWithToken(Unbox),
    0x7A -> PassWith(Throw),
    0x7B -> opCodeWithToken(LdFld),
    0x7C -> opCodeWithToken(LdFldA),
    0x7D -> opCodeWithToken(StFld),
    0x7E -> opCodeWithToken(LdSFld),
    0x7F -> opCodeWithToken(LdSFldA),

    0x80 -> opCodeWithToken(StSFld),
    0x81 -> opCodeWithToken(StObj),
    0x82 -> PassWith(ConvOvfI1Un),
    0x83 -> PassWith(ConvOvfI2Un),
    0x84 -> PassWith(ConvOvfI4Un),
    0x85 -> PassWith(ConvOvfI8Un),
    0x86 -> PassWith(ConvOvfU1Un),
    0x87 -> PassWith(ConvOvfU2Un),
    0x88 -> PassWith(ConvOvfU4Un),
    0x89 -> PassWith(ConvOvfU8Un),
    0x8A -> PassWith(ConvOvfIUn),
    0x8B -> PassWith(ConvOvfUUn),
    0x8C -> opCodeWithToken(Box),
    0x8D -> opCodeWithToken(NewArr),
    0x8E -> PassWith(LdLen),
    0x8F -> opCodeWithToken(LdElemA),

    0x90 -> PassWith(LdElemI1),
    0x91 -> PassWith(LdElemU1),
    0x92 -> PassWith(LdElemI2),
    0x93 -> PassWith(LdElemU2),
    0x94 -> PassWith(LdElemI4),
    0x95 -> PassWith(LdElemU4),
    0x96 -> PassWith(LdElemI8),
    0x97 -> PassWith(LdElemI),
    0x98 -> PassWith(LdElemR4),
    0x99 -> PassWith(LdElemR8),
    0x9A -> PassWith(LdElemRef),
    0x9B -> PassWith(StElemI),
    0x9C -> PassWith(StElemI1),
    0x9D -> PassWith(StElemI2),
    0x9E -> PassWith(StElemI4),
    0x9F -> PassWith(StElemI8),

    0xA0 -> PassWith(StElemR4),
    0xA1 -> PassWith(StElemR8),
    0xA2 -> PassWith(StElemRef),
    0xA3 -> opCodeWithToken(LdElem),
    0xA4 -> opCodeWithToken(StElem),
    0xA5 -> opCodeWithToken(UnboxAny),

    0xB3 -> PassWith(ConvOvfI1),
    0xB4 -> PassWith(ConvOvfU1),
    0xB5 -> PassWith(ConvOvfI2),
    0xB6 -> PassWith(ConvOvfU2),
    0xB7 -> PassWith(ConvOvfI4),
    0xB8 -> PassWith(ConvOvfU4),
    0xB9 -> PassWith(ConvOvfI8),
    0xBA -> PassWith(ConvOvfU8),

    0xC2 -> PassWith(RefAnyVal),
    0xC3 -> PassWith(CkFinite),
    0xC6 -> opCodeWithToken(MkRefAny),

    0xD0 -> opCodeWithToken(LdToken),
    0xD1 -> PassWith(ConvU2),
    0xD2 -> PassWith(ConvU1),
    0xD3 -> PassWith(ConvI),
    0xD4 -> PassWith(ConvOvfI),
    0xD5 -> PassWith(ConvOvfU),
    0xD6 -> PassWith(AddOvf),
    0xD7 -> PassWith(AddOvfUn),
    0xD8 -> PassWith(MullOvf),
    0xD9 -> PassWith(MullOvfUn),
    0xDA -> PassWith(SubOvf),
    0xDB -> PassWith(SubOvfUn),
    0xDC -> PassWith(EndFinaly),
    0xDD -> opCodeWithInt32(Leave),
    0xDE -> opCodeWithInt8(LeaveS),
    0xDF -> PassWith(StIndI),

    0xE0 -> PassWith(ConvU)
  )

  val parsersAfterFE: Map[Int, P[OpCode]] = Map(
    0x00 -> PassWith(ArgList),
    0x01 -> PassWith(Ceq),
    0x02 -> PassWith(Cgt),
    0x03 -> PassWith(CgtUn),
    0x04 -> PassWith(Clt),
    0x05 -> PassWith(CltUn),
    0x06 -> opCodeWithToken(LdFtn),
    0x07 -> opCodeWithToken(LdVirtFtn),
    //0x08
    0x09 -> opCodeWithInt32(LdArg),
    0x0A -> opCodeWithInt32(LdArgA),
    0x0B -> opCodeWithInt32(StArg),
    0x0C -> opCodeWithInt32(LdLoc),
    0x0D -> opCodeWithInt32(LdLocA),
    0x0E -> opCodeWithInt32(StLoc),
    0x0F -> PassWith(LocAlloc),

    0x11 -> PassWith(EndFilter),
    0x12 -> opCodeWithInt8(Unaligned),
    0x13 -> PassWith(Volatile),
    0x14 -> PassWith(Tail),
    0x15 -> PassWith(LdcI4M1),
    0x16 -> opCodeWithToken(InitObj),
    0x17 -> opCodeWithToken(Constrained),
    0x18 -> PassWith(CpBlk),
    0x19 -> PassWith(InitBlk),
    0x1A -> opCodeWithInt8(No),
    0x1B -> PassWith(ReThrow),
    0x1C -> PassWith(SizeOf),
    0x1D -> PassWith(RefAnyType),
    0x1E -> PassWith(Readonly)
  )

  val code: P[Seq[OpCode]] = {
    val oneByte = P( AnyByte.! ).flatMap(
      b => parsers1Byte(b.head & 0xff)
    )
    val twoBytes = P( BS(0xFE) ~ AnyByte.! ).flatMap(
      b => parsersAfterFE(b.head & 0xff)
    )

    P( (twoBytes | oneByte).rep )
  }
}
