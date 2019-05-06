/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.dotnet.parser

import fastparse.byte.all._
import pravda.dotnet.parser.CIL.CilData
import pravda.dotnet.utils._
import pravda.dotnet.data.TablesData._
import pravda.dotnet.data.{Heaps, TablesData}
import cats.instances.vector._
import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import pravda.dotnet.parser.Signatures.SigType.ArrayShape

// See http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-335.pdf p.258
// II.23.2 Blobs and signatures
object Signatures {

  import Heaps._

  sealed trait SigType {
    import SigType._

    def mkString: String = {
      def typeDefOrRefName(typeDefOrRef: TableRowData) = typeDefOrRef match {
        case TypeDefData(_, _, name, namespace, _, _, _) => s"$namespace.$name"
        case TypeRefData(_, name, namespace)             => s"$namespace.$name"
        case _                                           => ???
      }

      this match {
        case TypedByRef              => ???
        case SigType.Void            => "void"
        case SigType.Boolean         => "bool"
        case SigType.Char            => "char"
        case I1                      => "int8"
        case U1                      => "uint8"
        case I2                      => "int16"
        case U2                      => "uint16"
        case I4                      => "int32"
        case U4                      => "uint32"
        case I8                      => "int64"
        case U8                      => "uint64"
        case R4                      => "float"
        case R8                      => "double"
        case SigType.String          => "string"
        case I                       => "int"
        case U                       => "uint"
        case SigType.Object          => "object"
        case Cls(typeDefOrRef)       => typeDefOrRefName(typeDefOrRef)
        case ValueTpe(typeDefOrRef)  => typeDefOrRefName(typeDefOrRef)
        case Generic(tpe, tpeParams) => s"${tpe.mkString}<${tpeParams.map(_.mkString).mkString(", ")}>"
        case Var(num)                => s"var$num"
        case MVar(num)               => s"mvar$num"
        case Arr(tpe, shape)         => s"${tpe.mkString}[${shape.mkString}]"
      }
    }
  }

  object SigType {
    case object TypedByRef                                           extends SigType
    case object Void                                                 extends SigType
    case object Boolean                                              extends SigType
    case object Char                                                 extends SigType
    case object I1                                                   extends SigType
    case object U1                                                   extends SigType
    case object I2                                                   extends SigType
    case object U2                                                   extends SigType
    case object I4                                                   extends SigType
    case object U4                                                   extends SigType
    case object I8                                                   extends SigType
    case object U8                                                   extends SigType
    case object R4                                                   extends SigType
    case object R8                                                   extends SigType
    case object String                                               extends SigType
    case object I                                                    extends SigType
    case object U                                                    extends SigType
    case object Object                                               extends SigType
    final case class Cls(typeDefOrRef: TableRowData)                 extends SigType
    final case class ValueTpe(typeDefOrRef: TableRowData)            extends SigType
    final case class Generic(tpe: SigType, tpeParams: List[SigType]) extends SigType
    final case class Var(num: Int)                                   extends SigType
    final case class MVar(num: Int)                                  extends SigType

    final case class ArrayShape(rank: Int, sizes: List[Int], loBounds: List[Int]) {
      def mkString: String = s"$rank[${sizes.mkString(", ")}][${loBounds.mkString(", ")}]"
    }
    final case class Arr(tpe: SigType, shape: ArrayShape) extends SigType
  }

  sealed trait Signature

  final case class LocalVar(tpe: SigType, byRef: Boolean)
  final case class Tpe(tpe: SigType, byRef: Boolean)

  final case class LocalVarSig(types: List[LocalVar]) extends Signature
  final case class FieldSig(tpe: SigType)             extends Signature
  final case class MethodRefDefSig(instance: Boolean,
                                   explicit: Boolean,
                                   default: Boolean,
                                   vararg: Boolean,
                                   generics: Int,
                                   retType: Tpe,
                                   params: List[Tpe])
      extends Signature
  final case class TypeSig(tpe: Tpe) extends Signature

  private val arrayShape: P[SigType.ArrayShape] = {
    val sizes = compressedUInt.flatMap(num => compressedUInt.rep(exactly = num)).map(_.toList)
    val loBounds = compressedUInt.flatMap(num => compressedUInt.rep(exactly = num)).map(_.toList)

    P(compressedUInt ~ sizes ~ loBounds).map(ArrayShape.tupled)
  }

  def sigType(tablesData: TablesData): P[Either[String, SigType]] = {
    def simpleType(t: SigType): P[Either[String, SigType]] = PassWith(Right(t))

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
      case 0x13 => compressedUInt.map(i => Right(SigType.Var(i)))
      case 0x14 =>
        for {
          tpeV <- sigType(tablesData)
          shape <- arrayShape
        } yield tpeV.map(SigType.Arr(_, shape))
      case 0x15 =>
        for {
          tpeV <- sigType(tablesData)
          cnt <- compressedUInt
          tpesV <- sigType(tablesData).rep(exactly = cnt)
        } yield
          for {
            tpe <- tpeV
            tpes <- tpesV.toList.sequence
          } yield SigType.Generic(tpe, tpes)
      case 0x18 => simpleType(SigType.I)
      case 0x19 => simpleType(SigType.U)
      case 0x1c => simpleType(SigType.Object)
      case 0x1e => compressedUInt.map(i => Right(SigType.MVar(i)))
      case 0x1d =>
        for {
          tpeV <- sigType(tablesData)
        } yield tpeV.map(SigType.Arr(_, ArrayShape(1, List.empty, List.empty)))
      case c => PassWith(Left(s"Unknown type signature: 0x${c.toInt.toHexString}"))
    }
  }

  private def typeDefOrRef(tablesData: TablesData): P[Either[String, TableRowData]] =
    P(compressedUInt).map(i => {
      val mode = i & 0x03
      val idx = (i >> 2) - 1
      mode match {
        case 0 => tablesData.typeDefTable.lift(idx.toInt).toRight(s"Index out of TypeDef table bounds: $idx")
        case 1 => tablesData.typeRefTable.lift(idx.toInt).toRight(s"Index out of TypeRef table bounds: $idx")
        case 2 => Left("Unimplemented: TypeSpec table")
      }
    })

  private def localVar(tablesData: TablesData): P[Either[String, LocalVar]] = {
    val byRef = BS(0x10).!.?.map(_.isDefined)

    // FIXME Some fields are ignored, it might cause parsing errors
    P( /* CustomModes */ /* Constraints */ byRef ~ sigType(tablesData)).map {
      case (b, t) => t.map(LocalVar(_, b))
    }
  }

  def localVarSig(tablesData: TablesData): P[Either[String, LocalVarSig]] = {
    val typedByRef = BS(0x16).map(_ => Right(LocalVar(SigType.TypedByRef, false)))

    P(BS(0x07) ~ compressedUInt).flatMap(
      count =>
        P(typedByRef | localVar(tablesData))
          .rep(exactly = count.toInt)
          .map(tpes => tpes.toList.sequence.map(LocalVarSig))
    )
  }

  def fieldSig(tablesData: TablesData): P[Either[String, FieldSig]] =
    P(BS(0x06) ~ sigType(tablesData)).map(_.map(FieldSig))

  private def tpe(tablesData: TablesData): P[Either[String, Tpe]] = {
    val byRef = BS(0x10).!.?.map(_.isDefined)
    val typedByRef = BS(0x16).map(_ => Right(Tpe(SigType.TypedByRef, false)))
    val void = BS(0x01).map(_ => Right(Tpe(SigType.Void, false)))

    P(
      /* CustomMod */ (byRef ~ sigType(tablesData)).map { case (b, t) => t.map(Tpe(_, b)) } | typedByRef | void
    )
  }

  def methodRefDefSig(tablesData: TablesData): P[Either[String, MethodRefDefSig]] = {
    val instance = 0x20
    val explicity = 0x40
    val default = 0x00
    val vararg = 0x05
    val generic = 0x10

    for {
      b <- Int8
      genericP = if ((b & generic) != 0) compressedUInt else PassWith(0)
      gCount <- genericP
      paramCount <- compressedUInt
      retTpeV <- tpe(tablesData)
      paramsV <- tpe(tablesData).rep(exactly = paramCount)
    } yield
      for {
        retTpe <- retTpeV
        params <- paramsV.toList.sequence
      } yield
        MethodRefDefSig((b & instance) != 0,
                        (b & explicity) != 0,
                        (b & default) != 0,
                        (b & vararg) != 0,
                        gCount,
                        retTpe,
                        params)
  }

  def collectSignatures(cilData: CilData): Either[String, Map[Long, Signature]] = {
    def parseSignature[T <: Signature](idx: Long, p: P[Either[String, T]]): Either[String, T] =
      for {
        signatureBytes <- Heaps.blob(cilData.blobHeap, idx)
        signature <- p.parse(signatureBytes).toEither.joinRight
      } yield signature

    val idxToSig = cilData.tables.fieldTable.map(f =>
      f.signatureIdx -> parseSignature(f.signatureIdx, fieldSig(cilData.tables))) ++
      cilData.tables.memberRefTable.map(m =>
        m.signatureIdx -> parseSignature(m.signatureIdx, methodRefDefSig(cilData.tables) | fieldSig(cilData.tables))) ++
      cilData.tables.methodDefTable.map(m =>
        m.signatureIdx -> parseSignature(m.signatureIdx, methodRefDefSig(cilData.tables))) ++
      cilData.tables.standAloneSigTable.map(s =>
        s.signatureIdx -> parseSignature(s.signatureIdx, localVarSig(cilData.tables))) ++
      cilData.tables.typeSpecTable.map(s =>
        s.signatureIdx -> parseSignature(s.signatureIdx, tpe(cilData.tables).map(_.map(TypeSig))))

    idxToSig.map(_._2).sequence.map(idxToSig.map(_._1).zip(_)).map(_.toMap)
  }
}
