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

package pravda.dotnet.data

import pravda.dotnet.parsers.PE.Info._
import pravda.dotnet.parsers.TablesInfo._

import cats.instances.vector._
import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._

import pravda.dotnet.utils._

final case class TablesData(
    customAttributeTable: Vector[TablesData.CustomAttributeData],
    fieldTable: Vector[TablesData.FieldData],
    fieldRVATable: Vector[TablesData.FieldRVAData],
    memberRefTable: Vector[TablesData.MemberRefData],
    methodDefTable: Vector[TablesData.MethodDefData],
    paramTable: Vector[TablesData.ParamData],
    typeDefTable: Vector[TablesData.TypeDefData],
    typeRefTable: Vector[TablesData.TypeRefData],
    typeSpecTable: Vector[TablesData.TypeSpecData],
    standAloneSigTable: Vector[TablesData.StandAloneSigData],
    methodSpecTable: Vector[TablesData.MethodSpecData],
    documentTable: Vector[TablesData.DocumentData],
    methodDebugInformationTable: Vector[TablesData.MethodDebugInformationData]
) {

  def tableByNum(num: Int): Option[Vector[TablesData.TableRowData]] = num match {
    case 0x1  => Some(typeRefTable)
    case 0x2  => Some(typeDefTable)
    case 0x4  => Some(fieldTable)
    case 0x6  => Some(methodDefTable)
    case 0x8  => Some(paramTable)
    case 0xA  => Some(memberRefTable)
    case 0x11 => Some(standAloneSigTable)
    case 0x1B => Some(typeSpecTable)
    case 0x1D => Some(fieldRVATable)
    case 0x2B => Some(methodSpecTable)
    case _    => None
  }
}

object TablesData {

  sealed trait TableRowData
  sealed trait MethodRefDefData extends TableRowData {
    def name: String
    def signatureIdx: Long
  }
  final case class CustomAttributeData(parent: TableRowData, tpe: TableRowData /* value is ignored */ )
      extends TableRowData
  final case class MethodDefData(id: Int,
                                 implFlags: Short,
                                 flags: Short,
                                 name: String,
                                 signatureIdx: Long,
                                 params: Vector[ParamData])
      extends MethodRefDefData
  final case class MemberRefData(tableRowData: TableRowData, name: String, signatureIdx: Long) extends MethodRefDefData
  final case class FieldData(flags: Short, name: String, signatureIdx: Long)                   extends TableRowData
  final case class FieldRVAData(field: FieldData, rva: Long)                                   extends TableRowData
  final case class ParamData(flags: Short, seq: Int, name: String)                             extends TableRowData
  final case class TypeDefData(id: Int,
                               flags: Int,
                               name: String,
                               namespace: String,
                               var parent: TableRowData,
                               fields: Vector[FieldData],
                               methods: Vector[MethodDefData])
      extends TableRowData
  final case class TypeRefData(resolutionScopeIdx: Long, name: String, namespace: String) extends TableRowData
  final case class TypeSpecData(signatureIdx: Long)                                       extends TableRowData

  final case class StandAloneSigData(signatureIdx: Long) extends TableRowData

  final case class MethodSpecData(method: TableRowData, signatureIdx: Long) extends TableRowData

  final case class DocumentData(path: String)
  final case class MethodDebugInformationData(document: Option[String], points: List[Heaps.SequencePoint])
      extends TableRowData

  case object Ignored extends TableRowData

  def fromInfo(peData: PeData): Either[String, TablesData] = {

    def sizesFromIds(ids: Vector[Long], len: Long): Vector[Long] = {
      ids match {
        case rest :+ last =>
          val sizes = for {
            i <- rest.indices
          } yield ids(i + 1) - ids(i)
          sizes.toVector :+ (len - last + 1)
        case _ => Vector.empty
      }
    }

    val standAlongSigList = peData.tables.standAloneSigTable.map {
      case StandAloneSigRow(blobIdx) => StandAloneSigData(blobIdx)
    }

    val typeSpecList = peData.tables.typeSpecTable.map {
      case TypeSpecRow(idx) => TypeSpecData(idx)
    }

    val paramListV = peData.tables.paramTable.map {
      case ParamRow(flags, seq, nameIdx) =>
        for {
          name <- Heaps.string(peData.stringHeap, nameIdx)
        } yield ParamData(flags, seq, name)
    }.sequence

    val fieldListV = peData.tables.fieldTable.map {
      case FieldRow(flags, nameIdx, signatureIdx) =>
        for {
          name <- Heaps.string(peData.stringHeap, nameIdx)
        } yield FieldData(flags, name, signatureIdx)
    }.sequence

    val fieldRVAListV = peData.tables.fieldRVATable.map {
      case FieldRVARow(rva, fieldIdx) =>
        for {
          fieldList <- fieldListV
          field = fieldList(fieldIdx.toInt - 1)
        } yield FieldRVAData(field, rva)
    }.sequence

    def methodListV(paramList: Vector[ParamData]): Either[String, Vector[MethodDefData]] = {
      val paramListSizes = sizesFromIds(peData.tables.methodDefTable.map(_.paramListIdx), paramList.length.toLong)

      peData.tables.methodDefTable.zipWithIndex.map {
        case (MethodDefRow(_, implFlags, flags, nameIdx, signatureIdx, paramListIdx), i) =>
          for {
            name <- Heaps.string(peData.stringHeap, nameIdx)
          } yield
            MethodDefData(i,
                          implFlags,
                          flags,
                          name,
                          signatureIdx,
                          paramList.slice(paramListIdx.toInt - 1, paramListIdx.toInt + paramListSizes(i).toInt - 1))
      }.sequence
    }

    val typeRefListV = peData.tables.typeRefTable.map {
      case TypeRefRow(rs, nameIdx, namespaceIdx) =>
        for {
          name <- Heaps.string(peData.stringHeap, nameIdx)
          namespace <- Heaps.string(peData.stringHeap, namespaceIdx)
        } yield TypeRefData(rs, name, namespace)
    }.sequence

    def typeDefListV(fieldList: Vector[FieldData],
                     methodList: Vector[MethodDefData],
                     typeRefList: Vector[TypeRefData]): Either[String, Vector[TypeDefData]] = {
      val fieldListSizes = sizesFromIds(peData.tables.typeDefTable.map(_.fieldListIdx), fieldList.length.toLong)
      val methodListSizes = sizesFromIds(peData.tables.typeDefTable.map(_.methodListIdx), methodList.length.toLong)

      val typeDefRawE: Either[String, Vector[TypeDefData]] = peData.tables.typeDefTable.zipWithIndex.map {
        case (TypeDefRow(flags, nameIdx, namespaceIdx, parent, fieldListIdx, methodListIdx), i) =>
          for {
            name <- Heaps.string(peData.stringHeap, nameIdx)
            namespace <- Heaps.string(peData.stringHeap, namespaceIdx)
          } yield
            TypeDefData(
              i,
              flags,
              name,
              namespace,
              Ignored,
              fieldList.slice(fieldListIdx.toInt - 1, fieldListIdx.toInt + fieldListSizes(i).toInt - 1),
              methodList.slice(methodListIdx.toInt - 1, methodListIdx.toInt + methodListSizes(i).toInt - 1)
            )
      }.sequence

      for {
        typeDefList <- typeDefRawE
      } yield {
        peData.tables.typeDefTable.zipWithIndex.foreach {
          case (row, i) =>
            for {
              parent <- CodedIndexes.typeDefOrRef(row.parent, typeDefList, typeRefList, typeSpecList)
            } yield {
              typeDefList(i).parent = parent // FIXME we could implement more sophisticated algorithm
            }
        }
      }

      typeDefRawE
    }

    def memberRefListV(typeDefTable: Vector[TypeDefData],
                       typeRefTable: Vector[TypeRefData]): Either[String, Vector[MemberRefData]] =
      peData.tables.memberRefTable.map {
        case MemberRefRow(clsIdx, nameIdx, signatureIdx) =>
          for {
            name <- Heaps.string(peData.stringHeap, nameIdx)
            cls <- CodedIndexes.memberRefParent(clsIdx, typeDefTable, typeRefTable, typeSpecList)
          } yield MemberRefData(cls, name, signatureIdx)
      }.sequence

    def customAttributeListV(typeDefTable: Vector[TypeDefData],
                             methodDefTable: Vector[MethodDefData],
                             memberRefTable: Vector[MemberRefData]): Either[String, Vector[CustomAttributeData]] =
      peData.tables.customAttributeTable.map {
        case CustomAttributeRow(parentIdx, typeIdx, _) =>
          for {
            parent <- CodedIndexes.hasCustomAttribute(parentIdx, typeDefTable)
            tpe <- CodedIndexes.customAttributeType(typeIdx, methodDefTable, memberRefTable)
          } yield CustomAttributeData(parent, tpe)
      }.sequence

    def methodSpecListV(methodDefTable: Vector[MethodDefData],
                        memberRefTable: Vector[MemberRefData]): Either[String, Vector[MethodSpecData]] =
      peData.tables.methodSpecTable.map {
        case MethodSpecRow(methodIdx, signatureIdx) =>
          for {
            method <- CodedIndexes.methodDefOrRef(methodIdx, methodDefTable, memberRefTable)
          } yield MethodSpecData(method, signatureIdx)
      }.sequence

    val documentListV: Either[String, Vector[DocumentData]] =
      peData.tables.documentTable.map {
        case DocumentRow(nameIdx, _, _, _) =>
          for {
            nameBlob <- Heaps.blob(peData.blobHeap, nameIdx)
            nameRes <- Heaps.documentName.parse(nameBlob).toEither
            (sep, parts) = nameRes
            partsBlobs <- parts.map(p => Heaps.blob(peData.blobHeap, p.toLong)).sequence
            partsStrs <- partsBlobs.map(b => Heaps.blobUtf8.parse(b).toEither).sequence
          } yield DocumentData(partsStrs.mkString(sep))
      }.sequence

    def methodDebugInformationListV(documentList: Vector[DocumentData]) =
      peData.tables.methodDebugInformationTable.map {
        case MethodDebugInformationRow(documentIdx, seqPoints) =>
          // if documentIdx is 0 than we skip one compressedUInt in header
          // it's not implemented yet
          // possibly it's very rare and won't be a problem
          val document = if (documentIdx == 0) None else Some(documentList(documentIdx.toInt - 1).path)

          if (seqPoints != 0) {
            for {
              blob <- Heaps.blob(peData.blobHeap, seqPoints)
              seqPoints <- Heaps.sequencePoints.parse(blob).toEither
            } yield {
              MethodDebugInformationData(document, seqPoints)
            }
          } else {
            Right(MethodDebugInformationData(document, List.empty))
          }
      }.sequence

    for {
      paramList <- paramListV
      fieldList <- fieldListV
      fieldRVAList <- fieldRVAListV
      methodList <- methodListV(paramList)
      typeRefList <- typeRefListV
      typeDefList <- typeDefListV(fieldList, methodList, typeRefList)
      memberRefList <- memberRefListV(typeDefList, typeRefList)
      customAttributeList <- customAttributeListV(typeDefList, methodList, memberRefList)
      methodSpecList <- methodSpecListV(methodList, memberRefList)
      documentList <- documentListV
      methodDebugInformationList <- methodDebugInformationListV(documentList)
    } yield
      TablesData(
        customAttributeList,
        fieldList,
        fieldRVAList,
        memberRefList,
        methodList,
        paramList,
        typeDefList,
        typeRefList,
        typeSpecList,
        standAlongSigList,
        methodSpecList,
        documentList,
        methodDebugInformationList
      )
  }
}
