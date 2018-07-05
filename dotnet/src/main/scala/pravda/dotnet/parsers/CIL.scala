package pravda.dotnet.parsers

import fastparse.byte.all._
import LE._
import pravda.dotnet.data.{Heaps, TablesData}

import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._

object CIL {
  final case class CilData(stringHeap: Bytes,
                           userStringHeap: Bytes,
                           blobHeap: Bytes,
                           tableNumbers: List[Int],
                           tables: TablesData)

  def fromPeData(peData: PE.Info.PeData): Either[String, CilData] =
    TablesData
      .fromInfo(peData)
      .map(tables => CilData(peData.stringHeap, peData.userStringHeap, peData.blobHeap, peData.tableNumbers, tables))

  type Token = TablesData.TableRowData

  sealed trait Op {
    def size: Int = 1
  }

  // prefixes
  final case class Constrained(token: Token) extends Op {
    override val size = 6
  }
  final case class No(byte: Byte) extends Op {
    override val size = 3
  }
  case object Readonly extends Op {
    override val size = 2
  }
  case object Tail extends Op {
    override val size = 2
  }
  final case class Unaligned(alignment: Byte) extends Op {
    override val size = 3
  }
  case object Volatile extends Op {
    override val size = 2
  }

  // base instructions
  case object Add      extends Op
  case object AddOvf   extends Op
  case object AddOvfUn extends Op
  case object And      extends Op
  case object ArgList extends Op {
    override val size = 2
  }

  final case class Beq(target: Int) extends Op {
    override val size = 5
  }
  final case class BeqS(target: Byte) extends Op {
    override val size = 2
  }
  final case class Bge(target: Int) extends Op {
    override val size = 5
  }
  final case class BgeS(target: Byte) extends Op {
    override val size = 2
  }
  final case class BgeUn(target: Int) extends Op {
    override val size = 5
  }
  final case class BgeUnS(target: Byte) extends Op {
    override val size = 2
  }
  final case class Bgt(target: Int) extends Op {
    override val size = 5
  }
  final case class BgtS(target: Byte) extends Op {
    override val size = 2
  }
  final case class BgtUn(target: Int) extends Op {
    override val size = 5
  }
  final case class BgtUnS(target: Byte) extends Op {
    override val size = 2
  }
  final case class Ble(target: Int) extends Op {
    override val size = 5
  }
  final case class BleS(target: Byte) extends Op {
    override val size = 2
  }
  final case class BleUn(target: Int) extends Op {
    override val size = 5
  }
  final case class BleUnS(target: Byte) extends Op {
    override val size = 2
  }
  final case class Blt(target: Int) extends Op {
    override val size = 5
  }
  final case class BltS(target: Byte) extends Op {
    override val size = 2
  }
  final case class BltUn(target: Int) extends Op {
    override val size = 5
  }
  final case class BltUnS(target: Byte) extends Op {
    override val size = 2
  }
  final case class BneUn(target: Int) extends Op {
    override val size = 5
  }
  final case class BneUnS(target: Byte) extends Op {
    override val size = 2
  }
  final case class Br(target: Int) extends Op {
    override val size = 5
  }
  final case class BrS(target: Byte) extends Op {
    override val size = 2
  }

  case object Break extends Op

  final case class BrFalse(target: Int) extends Op {
    override val size = 5
  }
  final case class BrFalseS(target: Byte) extends Op {
    override val size = 2
  }
  final case class BrNull(target: Int) extends Op {
    override val size = 5
  }
  final case class BrNullS(target: Byte) extends Op {
    override val size = 2
  }
  final case class BrZero(target: Int) extends Op {
    override val size = 5
  }
  final case class BrZeroS(target: Byte) extends Op {
    override val size = 2
  }

  final case class BrTrue(target: Int) extends Op {
    override val size = 5
  }
  final case class BrTrueS(target: Byte) extends Op {
    override val size = 2
  }
  final case class BrInst(target: Int) extends Op {
    override val size = 5
  }
  final case class BrInstS(target: Byte) extends Op {
    override val size = 2
  }

  final case class Call(token: Token) extends Op {
    override val size = 5
  }
  final case class CallI(token: Token) extends Op {
    override val size: Int = 5
  }

  case object Ceq extends Op {
    override val size = 2
  }
  case object Cgt extends Op {
    override val size = 2
  }
  case object CgtUn extends Op {
    override val size = 2
  }
  case object CkFinite extends Op
  case object Clt extends Op {
    override val size = 2
  }
  case object CltUn extends Op {
    override val size = 2
  }

  case object ConvI1  extends Op
  case object ConvI2  extends Op
  case object ConvI4  extends Op
  case object ConvI8  extends Op
  case object ConvR4  extends Op
  case object ConvR8  extends Op
  case object ConvU1  extends Op
  case object ConvU2  extends Op
  case object ConvU4  extends Op
  case object ConvU8  extends Op
  case object ConvI   extends Op
  case object ConvU   extends Op
  case object ConvRUn extends Op

  case object ConvOvfI1 extends Op
  case object ConvOvfI2 extends Op
  case object ConvOvfI4 extends Op
  case object ConvOvfI8 extends Op
  case object ConvOvfU1 extends Op
  case object ConvOvfU2 extends Op
  case object ConvOvfU4 extends Op
  case object ConvOvfU8 extends Op
  case object ConvOvfI  extends Op
  case object ConvOvfU  extends Op

  case object ConvOvfI1Un extends Op
  case object ConvOvfI2Un extends Op
  case object ConvOvfI4Un extends Op
  case object ConvOvfI8Un extends Op
  case object ConvOvfU1Un extends Op
  case object ConvOvfU2Un extends Op
  case object ConvOvfU4Un extends Op
  case object ConvOvfU8Un extends Op
  case object ConvOvfIUn  extends Op
  case object ConvOvfUUn  extends Op

  case object CpBlk extends Op {
    override val size = 2
  }

  case object Div   extends Op
  case object DivUn extends Op
  case object Dup   extends Op

  case object EndFilter extends Op {
    override val size = 2
  }
  case object EndFinaly extends Op

  case object InitBlk extends Op {
    override val size = 2
  }

  final case class Jmp(token: Token) extends Op {
    override val size = 5
  }

  final case class LdArg(num: Int) extends Op {
    override val size: Int = 6
  }
  final case class LdArgS(num: Short) extends Op {
    override val size = 2
  }
  case object LdArg0 extends Op
  case object LdArg1 extends Op
  case object LdArg2 extends Op
  case object LdArg3 extends Op
  final case class LdArgA(num: Int) extends Op {
    override val size = 6
  }
  final case class LdArgAS(num: Short) extends Op {
    override val size = 2
  }

  final case class LdcI4(num: Int) extends Op {
    override val size = 5
  }
  final case class LdcI8(num: Long) extends Op {
    override val size = 9
  }
  final case class LdcR4(num: Float) extends Op {
    override val size = 5
  }
  final case class LdcR8(num: Double) extends Op {
    override val size = 9
  }
  case object LdcI40 extends Op
  case object LdcI41 extends Op
  case object LdcI42 extends Op
  case object LdcI43 extends Op
  case object LdcI44 extends Op
  case object LdcI45 extends Op
  case object LdcI46 extends Op
  case object LdcI47 extends Op
  case object LdcI48 extends Op
  case object LdcI4M1 extends Op {
    override val size = 2
  }
  final case class LdcI4S(num: Byte) extends Op {
    override val size = 2
  }

  final case class LdFtn(token: Token) extends Op {
    override val size = 6
  }

  case object LdIndI1  extends Op
  case object LdIndI2  extends Op
  case object LdIndI4  extends Op
  case object LdIndI8  extends Op
  case object LdIndU1  extends Op
  case object LdIndU2  extends Op
  case object LdIndU4  extends Op
  case object LdIndR4  extends Op
  case object LdIndR8  extends Op
  case object LdIndI   extends Op
  case object LdIndRef extends Op

  final case class LdLoc(num: Int) extends Op {
    override val size = 6
  }
  final case class LdLocS(num: Short) extends Op {
    override val size = 2
  }
  case object LdLoc0 extends Op
  case object LdLoc1 extends Op
  case object LdLoc2 extends Op
  case object LdLoc3 extends Op
  final case class LdLocA(num: Int) extends Op {
    override val size = 6
  }
  final case class LdLocAS(num: Short) extends Op {
    override val size = 2
  }

  case object LdNull extends Op

  final case class Leave(target: Int) extends Op {
    override val size = 5
  }
  final case class LeaveS(target: Byte) extends Op {
    override val size = 2
  }

  case object LocAlloc extends Op {
    override val size = 2
  }

  case object Mul      extends Op
  case object MulOvf   extends Op
  case object MulOvfUn extends Op

  case object Neg   extends Op
  case object Nop   extends Op
  case object Not   extends Op
  case object Or    extends Op
  case object Pop   extends Op
  case object Rem   extends Op
  case object RemUn extends Op
  case object Ret   extends Op
  case object Shl   extends Op
  case object Shr   extends Op
  case object ShrUn extends Op

  final case class StArg(num: Int) extends Op {
    override val size = 5
  }
  final case class StArgS(num: Short) extends Op {
    override val size = 2
  }

  case object StIndI1  extends Op
  case object StIndI2  extends Op
  case object StIndI4  extends Op
  case object StIndI8  extends Op
  case object StIndR4  extends Op
  case object StIndR8  extends Op
  case object StIndI   extends Op
  case object StIndRef extends Op

  final case class StLoc(num: Int) extends Op {
    override val size = 6
  }
  final case class StLocS(num: Short) extends Op {
    override val size = 2
  }
  case object StLoc0 extends Op
  case object StLoc1 extends Op
  case object StLoc2 extends Op
  case object StLoc3 extends Op

  case object Sub      extends Op
  case object SubOvf   extends Op
  case object SubOvfUn extends Op

  final case class Switch(targets: List[Int]) extends Op {
    override val size = 1 + targets.length * 4
  }

  case object Xor extends Op

  // object instructions

  final case class Box(typeToken: Token) extends Op {
    override val size = 5
  }
  final case class CallVirt(methodToken: Token) extends Op {
    override val size = 5
  }
  final case class CastClass(typeToken: Token) extends Op {
    override val size = 5
  }
  final case class CpObj(typeToken: Token) extends Op {
    override val size = 5
  }
  final case class InitObj(typeToken: Token) extends Op {
    override val size = 5
  }
  final case class IsInst(typeToken: Token) extends Op {
    override val size = 5
  }

  final case class LdElem(typeToken: Token) extends Op {
    override val size = 5
  }
  case object LdElemI1  extends Op
  case object LdElemI2  extends Op
  case object LdElemI4  extends Op
  case object LdElemI8  extends Op
  case object LdElemU1  extends Op
  case object LdElemU2  extends Op
  case object LdElemU4  extends Op
  case object LdElemU8  extends Op
  case object LdElemR4  extends Op
  case object LdElemR8  extends Op
  case object LdElemI   extends Op
  case object LdElemRef extends Op
  final case class LdElemA(typeToken: Token) extends Op {
    override val size = 5
  }

  final case class LdFld(field: Token) extends Op {
    override val size = 5
  }
  final case class LdFldA(field: Token) extends Op {
    override val size = 5
  }
  case object LdLen extends Op
  final case class LdObj(typeToken: Token) extends Op {
    override val size = 5
  }
  final case class LdSFld(field: Token) extends Op {
    override val size = 5
  }
  final case class LdSFldA(field: Token) extends Op {
    override val size = 5
  }
  final case class LdStr(string: String) extends Op {
    override val size = 5
  }
  final case class LdToken(token: Token) extends Op {
    override val size = 5
  }
  final case class LdVirtFtn(method: Token) extends Op {
    override val size = 6
  }

  final case class MkRefAny(cls: Token) extends Op {
    override val size = 5
  }
  final case class NewArr(etype: Token) extends Op {
    override val size = 5
  }
  final case class NewObj(ctor: Token) extends Op {
    override val size = 5
  }

  case object RefAnyType extends Op {
    override val size = 2
  }
  final case class RefAnyVal(typeToken: Token) extends Op {
    override val size = 5
  }

  case object ReThrow extends Op {
    override val size = 2
  }
  final case class SizeOf(typeToken: Token) extends Op {
    override val size = 6
  }

  final case class StElem(typeToken: Token) extends Op {
    override val size = 5
  }
  case object StElemI1  extends Op
  case object StElemI2  extends Op
  case object StElemI4  extends Op
  case object StElemI8  extends Op
  case object StElemR4  extends Op
  case object StElemR8  extends Op
  case object StElemI   extends Op
  case object StElemRef extends Op

  final case class StFld(field: Token) extends Op {
    override val size = 5
  }
  final case class StObj(typeToken: Token) extends Op {
    override val size = 5
  }
  final case class StSFld(field: Token) extends Op {
    override val size = 5
  }

  case object Throw extends Op
  final case class Unbox(valueType: Token) extends Op {
    override val size = 5
  }
  final case class UnboxAny(typeToken: Token) extends Op {
    override val size = 5
  }

  // Syntetic OpCodes used when translating CIL to assembler
  final case class Jump(name: String) extends Op {
    override val size = 0
  }
  final case class JumpI(name: String) extends Op {
    override val size = 0
  }
  final case class Label(name: String) extends Op {
    override val size = 0
  }

  private def opCode[T](t: T): P[Either[String, T]] = PassWith(Right(t))
  private def opCodeWithUInt8[T](convert: Short => T): P[Either[String, T]] = P(UInt8).map(convert.andThen(Right(_)))
  private def opCodeWithInt8[T](convert: Byte => T): P[Either[String, T]] = P(Int8).map(convert.andThen(Right(_)))
  private def opCodeWithInt32[T](convert: Int => T): P[Either[String, T]] = P(Int32).map(convert.andThen(Right(_)))
  private def opCodeWithToken[T](convert: Token => T, cilData: CilData): P[Either[String, T]] = P(Int32).map(
    t => {
      val tableIdx = t >> 24
      val idx = t & 0x00ffffff
      val token = cilData.tables.tableByNum(tableIdx).map(_(idx - 1)).getOrElse(TablesData.Ignored)
      Right(convert(token))
    }
  )
  private def opCodeWithString[T](convert: String => T, cilData: CilData): P[Either[String, T]] = P(Int32).map(
    t => {
      if ((t >> 24) == 0x07) {
        Left(s"wrong token first byte: ${(t >> 24).toHexString}")
      } else {
        val idx = t & 0x00ffffff
        Heaps.userString(cilData.userStringHeap, idx.toLong).map(convert)
      }
    }
  )

  def parsers1Byte(cilData: CilData): Map[Int, P[Either[String, Op]]] = Map(
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
    0x21 -> P(Int64).map(LdcI8.andThen(Right(_))),
    0x22 -> P(Float32).map(LdcR4.andThen(Right(_))),
    0x23 -> P(Float64).map(LdcR8.andThen(Right(_))),
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
    0x45 -> P(UInt32).flatMap(l => Int32.rep(exactly = l.toInt).map(_.toList).map(Switch.andThen(Right(_)))),
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
    0x5A -> opCode(Mul),
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
    0xD8 -> opCode(MulOvf),
    0xD9 -> opCode(MulOvfUn),
    0xDA -> opCode(SubOvf),
    0xDB -> opCode(SubOvfUn),
    0xDC -> opCode(EndFinaly),
    0xDD -> opCodeWithInt32(Leave),
    0xDE -> opCodeWithInt8(LeaveS),
    0xDF -> opCode(StIndI),
    0xE0 -> opCode(ConvU)
  )

  def parsersAfterFE(cilData: CilData): Map[Int, P[Either[String, Op]]] = Map(
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

  def code(cilData: CilData): P[Either[String, List[Op]]] = {
    val p1 = parsers1Byte(cilData)
    val p2 = parsersAfterFE(cilData)

    val oneByte = P(AnyByte.!).flatMap(
      b => p1(b.head & 0xff)
    )
    val twoBytes = P(BS(0xFE) ~ AnyByte.!).flatMap(
      b => p2(b.head & 0xff)
    )

    P((twoBytes | oneByte).rep).map(_.toList.sequence)
  }
}
