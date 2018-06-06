package pravda.dotnet

import fastparse.byte.all._
import pravda.dotnet.CIL.CilData
import pravda.dotnet.TablesData.TableRowData
import pravda.dotnet.utils._

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
    final case class ValueTpe(typeDefOrRef: TableRowData)           extends SigType
    final case class Generic(tpe: SigType, tpeParams: Seq[SigType]) extends SigType
    final case class Var(num: Long)                                 extends SigType
    // FIXME complex types are ignored
  }

  sealed trait Signature

  final case class LocalVar(tpe: SigType, byRef: Boolean)
  final case class Tpe(tpe: SigType, byRef: Boolean)

  final case class LocalVarSig(types: Seq[LocalVar]) extends Signature
  final case class FieldSig(tpe: SigType)            extends Signature
  final case class MethodRefDefSig(instance: Boolean,
                                   explicit: Boolean,
                                   default: Boolean,
                                   vararg: Boolean,
                                   generics: Int,
                                   retType: Tpe,
                                   params: Seq[Tpe])
      extends Signature
  final case class TypeSig(tpe: Tpe) extends Signature

  private val compressedUInt: P[Long] = P(Int8).flatMap(b => {
    if ((b & 0x80) == 0) {
      PassWith(b.toLong)
    } else if ((b & 0x40) == 0) {
      P(Int8).map(b2 => (((b & 0x30) << 8) + b2).toLong)
    } else {
      P(Int8 ~ Int8 ~ Int8).map { case (b2, b3, b4) => (((b & 0x10) << 24) + (b2 << 16) + (b3 << 8) + b4).toLong }
    }
  })

  def sigType(tablesData: TablesData): P[Validated[SigType]] = {
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
      case 0x11 => typeDefOrRef(tablesData).map(_.map(SigType.ValueTpe))
      case 0x12 => typeDefOrRef(tablesData).map(_.map(SigType.Cls))
      case 0x13 => compressedUInt.map(i => validated(SigType.Var(i)))
      case 0x15 =>
        for {
          tpeV <- sigType(tablesData)
          cnt <- compressedUInt
          tpesV <- sigType(tablesData).rep(exactly = cnt.toInt)
        } yield
          for {
            tpe <- tpeV
            tpes <- tpesV.sequence
          } yield SigType.Generic(tpe, tpes)
      case 0x18 => simpleType(SigType.I)
      case 0x19 => simpleType(SigType.U)
      case c =>
        throw new NotImplementedError
    }
  }

  private def typeDefOrRef(tablesData: TablesData): P[Validated[TableRowData]] =
    P(compressedUInt).map(i => {
      val mode = i & 0x03
      val idx = (i >> 2) - 1
      mode match {
        case 0 => tablesData.typeDefTable.lift(idx.toInt).toValidated(s"Index out of TypeDef table bounds: $idx")
        case 1 => tablesData.typeRefTable.lift(idx.toInt).toValidated(s"Index out of TypeRef table bounds: $idx")
        case 2 => validationError("Unimplemented: TypeSpec table")
      }
    })

  private def localVar(tablesData: TablesData): P[Validated[LocalVar]] = {
    val byRef = BS(0x10).!.?.map(_.isDefined)

    // FIXME Some fields are ignored, it might cause parsing errors
    P( /* CustomModes */ /* Constraints */ byRef ~ sigType(tablesData)).map {
      case (b, t) => t.map(LocalVar(_, b))
    }
  }

  def localVarSig(tablesData: TablesData): P[Validated[LocalVarSig]] = {
    val typedByRef = BS(0x16).map(_ => validated(LocalVar(SigType.TypedByRef, false)))

    P(BS(0x07) ~ compressedUInt).flatMap(
      count =>
        P(typedByRef | localVar(tablesData))
          .rep(exactly = count.toInt)
          .map(tpes => tpes.sequence.map(LocalVarSig))
    )
  }

  def fieldSig(tablesData: TablesData): P[Validated[FieldSig]] = P(BS(0x06) ~ sigType(tablesData)).map(_.map(FieldSig))

  private def tpe(tablesData: TablesData): P[Validated[Tpe]] = {
    val byRef = BS(0x10).!.?.map(_.isDefined)
    val typedByRef = BS(0x16).map(_ => validated(Tpe(SigType.TypedByRef, false)))
    val void = BS(0x01).map(_ => validated(Tpe(SigType.Void, false)))

    P(
      /* CustomMod */ (byRef ~ sigType(tablesData)).map { case (b, t) => t.map(Tpe(_, b)) } | typedByRef | void
    )
  }

  def methodRefDefSig(tablesData: TablesData): P[Validated[MethodRefDefSig]] = {
    val instance = 0x20
    val explicity = 0x40
    val default = 0x00
    val vararg = 0x05
    val generic = 0x10

    for {
      b <- Int8
      genericP = if ((b & generic) != 0) compressedUInt else PassWith(0L)
      gCount <- genericP
      paramCount <- compressedUInt
      retTpeV <- tpe(tablesData)
      paramsV <- tpe(tablesData).rep(exactly = paramCount.toInt)
    } yield
      for {
        retTpe <- retTpeV
        params <- paramsV.sequence
      } yield
        MethodRefDefSig((b & instance) != 0,
                        (b & explicity) != 0,
                        (b & default) != 0,
                        (b & vararg) != 0,
                        gCount.toInt,
                        retTpe,
                        params)
  }

  def collectSignatures(cilData: CilData): Validated[Map[Long, Signature]] = {
    def parseSignature[T <: Signature](idx: Long, p: P[Validated[T]]): Validated[T] =
      for {
        signatureBytes <- Heaps.blob(cilData.blobHeap, idx)
        signature <- p.parse(signatureBytes).toValidated.joinRight
      } yield signature

    val idxToSig = cilData.tables.fieldTable.map(f =>
      f.signatureIdx -> parseSignature(f.signatureIdx, fieldSig(cilData.tables))) ++
      cilData.tables.memberRefTable.map(m =>
        m.signatureIdx -> parseSignature(m.signatureIdx, methodRefDefSig(cilData.tables))) ++
      cilData.tables.methodDefTable.map(m =>
        m.signatureIdx -> parseSignature(m.signatureIdx, methodRefDefSig(cilData.tables))) ++
      cilData.tables.standAloneSigTable.map(s =>
        s.signatureIdx -> parseSignature(s.signatureIdx, localVarSig(cilData.tables))) ++
      cilData.tables.typeSpecTable.map(s =>
        s.signatureIdx -> parseSignature(s.signatureIdx, tpe(cilData.tables).map(_.map(TypeSig))))

    idxToSig.map(_._2).sequence.map(idxToSig.map(_._1).zip(_)).map(_.toMap)
  }
}
