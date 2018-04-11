package io.mytc.sood.cil

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
  case object MullUn extends OpCode

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
}
