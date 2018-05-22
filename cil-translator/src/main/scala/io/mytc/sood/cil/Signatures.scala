package io.mytc.sood.cil

import fastparse.byte.all._
import io.mytc.sood.cil.CIL.CilData
import io.mytc.sood.cil.TablesData.TableRowData
import io.mytc.sood.cil.utils._

object Signatures {

  sealed trait SigType

  object SigType {
    case object TypedByRef                                          extends SigType
    case object Void                                                extends SigType
    case object Boolean                                             extends SigType
    case object Char                                                extends SigType
    case object I1                                                  extends SigType
    case object U1                                                  extends SigType
    case object I2                                                  extends SigType
    case object U2                                                  extends SigType
    case object I4                                                  extends SigType
    case object U4                                                  extends SigType
    case object I8                                                  extends SigType
    case object U8                                                  extends SigType
    case object R4                                                  extends SigType
    case object R8                                                  extends SigType
    case object String                                              extends SigType
    case object I                                                   extends SigType
    case object U                                                   extends SigType
    final case class Cls(typeDefOrRef: TableRowData)                extends SigType
    final case class Generic(tpe: SigType, tpeParams: Seq[SigType]) extends SigType
    // FIXME complex types are ignored
  }

  sealed trait Signature

  final case class LocalVar(tpe: SigType, byRef: Boolean)
  final case class LocalVarSig(types: Seq[LocalVar]) extends Signature

  final case class FieldSig(tpe: SigType) extends Signature

  private val compressedUInt: P[Long] = P(Int8).flatMap(b => {
    if ((b & 0x80) == 0) {
      PassWith(b.toLong)
    } else if ((b & 0x40) == 0) {
      P(Int8).map(b2 => (((b & 0x30) << 8) + b2).toLong)
    } else {
      P(Int8 ~ Int8 ~ Int8).map { case (b2, b3, b4) => (((b & 0x10) << 24) + (b2 << 16) + (b3 << 8) + b4).toLong }
    }
  })

  def sigType(cilData: CilData): P[Validated[SigType]] = {
    def simpleType(t: SigType): P[Validated[SigType]] = PassWith(validated(t))

    P(Int8).flatMap {
      case 0x01 => simpleType(SigType.Void)
      case 0x02 => simpleType(SigType.Boolean)
      case 0x03 => simpleType(SigType.Char)
      case 0x04 => simpleType(SigType.I1)
      case 0x05 => simpleType(SigType.U1)
      case 0x06 => simpleType(SigType.I2)
      case 0x07 => simpleType(SigType.U2)
      case 0x08 => simpleType(SigType.I4)
      case 0x09 => simpleType(SigType.U4)
      case 0x0A => simpleType(SigType.I8)
      case 0x0B => simpleType(SigType.U8)
      case 0x0C => simpleType(SigType.R4)
      case 0x0D => simpleType(SigType.R8)
      case 0x0E => simpleType(SigType.String)
      case 0x18 => simpleType(SigType.I)
      case 0x19 => simpleType(SigType.U)
      case 0x12 => typeDefOrRef(cilData).map(_.map(SigType.Cls))
      case 0x15 =>
        for {
          tpeV <- sigType(cilData)
          cnt <- compressedUInt
          tpesV <- sigType(cilData).rep(exactly = cnt.toInt)
        } yield
          for {
            tpe <- tpeV
            tpes <- tpesV.sequence
          } yield SigType.Generic(tpe, tpes)
      case _ => throw new NotImplementedError
    }
  }

  private def typeDefOrRef(cilData: Table): P[Validated[TableRowData]] =
    P(compressedUInt).map(i => {
      val mode = i & 0x03
      val idx = (i >> 2) - 1
      mode match {
        case 0 => cilData.tables.typeDefTable.lift(idx.toInt).toValidated(s"Index out of TypeDef table bounds: $idx")
        case 1 => validationError("Unimplemented: TypeRef table")
        case 2 => validationError("Unimplemented: TypeSpec table")
      }
    })

  private def localVar(cilData: CilData): P[Validated[LocalVar]] =
    // FIXME Some fields are ignored, it might cause parsing errors
    P( /* CustomModes */ /* Constraints */ BS(0x10).!.?.map(_.isDefined) ~ sigType(cilData)).map {
      case (b, t) => t.map(LocalVar(_, b))
    }
  def localVarSig(cilData: CilData): P[Validated[LocalVarSig]] = P(BS(0x07) ~ compressedUInt).flatMap(
    count =>
      P(BS(0x16).map(_ => validated(LocalVar(SigType.TypedByRef, false))) | localVar(cilData))
        .rep(exactly = count.toInt)
        .map(tpes => tpes.sequence.map(LocalVarSig))
  )

  def fieldSig(cilData: CilData): P[Validated[FieldSig]] = P(BS(0x06) ~ sigType(cilData)).map(_.map(FieldSig))
}
