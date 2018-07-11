package pravda.dotnet.data

import pravda.dotnet.parsers.PE.Info._
import pravda.dotnet.parsers.TablesInfo._

import cats.instances.vector._
import cats.instances.either._
import cats.syntax.traverse._

final case class TablesData(
    fieldTable: Vector[TablesData.FieldData],
    fieldRVATable: Vector[TablesData.FieldRVAData],
    memberRefTable: Vector[TablesData.MemberRefData],
    methodDefTable: Vector[TablesData.MethodDefData],
    paramTable: Vector[TablesData.ParamData],
    typeDefTable: Vector[TablesData.TypeDefData],
    typeRefTable: Vector[TablesData.TypeRefData],
    typeSpecTable: Vector[TablesData.TypeSpecData],
    standAloneSigTable: Vector[TablesData.StandAloneSigData]
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
    case _    => None
  }
}

object TablesData {

  sealed trait TableRowData
  final case class MethodDefData(implFlags: Short,
                                 flags: Short,
                                 name: String,
                                 signatureIdx: Long,
                                 params: Vector[ParamData])
      extends TableRowData
  final case class MemberRefData(tableRowData: TableRowData, name: String, signatureIdx: Long) extends TableRowData
  final case class FieldData(flags: Short, name: String, signatureIdx: Long)                   extends TableRowData
  final case class FieldRVAData(field: FieldData, rva: Long)                                   extends TableRowData
  final case class ParamData(flags: Short, seq: Int, name: String)                             extends TableRowData
  final case class TypeDefData(flags: Int,
                               name: String,
                               namespace: String,
                               parent: TableRowData,
                               fields: Vector[FieldData],
                               methods: Vector[MethodDefData])
      extends TableRowData
  final case class TypeRefData(resolutionScopeIdx: Long, name: String, namespace: String) extends TableRowData
  final case class TypeSpecData(signatureIdx: Long)                                       extends TableRowData

  final case class StandAloneSigData(signatureIdx: Long) extends TableRowData
  case object Ignored                                    extends TableRowData

  def fromInfo(peData: PeData): Either[String, TablesData] = {

    def sizesFromIds(ids: Vector[Long]): Vector[Long] =
      ids match {
        case rest :+ last =>
          val sizes = for {
            i <- rest.indices
          } yield ids(i + 1) - ids(i)
          sizes.toVector :+ (ids.length - last)
        case _ => Vector.empty
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
      val paramListSizes = sizesFromIds(peData.tables.methodDefTable.map(_.paramListIdx))

      peData.tables.methodDefTable.zipWithIndex.map {
        case (MethodDefRow(_, implFlags, flags, nameIdx, signatureIdx, paramListIdx), i) =>
          for {
            name <- Heaps.string(peData.stringHeap, nameIdx)
          } yield
            MethodDefData(implFlags,
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
                     methodList: Vector[MethodDefData]): Either[String, Vector[TypeDefData]] = {
      val fieldListSizes = sizesFromIds(peData.tables.typeDefTable.map(_.fieldListIdx))
      val methodListSizes = sizesFromIds(peData.tables.typeDefTable.map(_.methodListIdx))

      peData.tables.typeDefTable.zipWithIndex.map {
        case (TypeDefRow(flags, nameIdx, namespaceIdx, parent, fieldListIdx, methodListIdx), i) =>
          for {
            name <- Heaps.string(peData.stringHeap, nameIdx)
            namespace <- Heaps.string(peData.stringHeap, namespaceIdx)
          } yield
            TypeDefData(
              flags,
              name,
              namespace,
              Ignored,
              fieldList.slice(fieldListIdx.toInt - 1, fieldListIdx.toInt + fieldListSizes(i).toInt - 1),
              methodList.slice(methodListIdx.toInt - 1, methodListIdx.toInt + methodListSizes(i).toInt - 1)
            )
      }.sequence
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

    for {
      paramList <- paramListV
      fieldList <- fieldListV
      fieldRVAList <- fieldRVAListV
      methodList <- methodListV(paramList)
      typeDefList <- typeDefListV(fieldList, methodList)
      typeRefList <- typeRefListV
      memberRefList <- memberRefListV(typeDefList, typeRefList)
    } yield
      TablesData(fieldList,
                 fieldRVAList,
                 memberRefList,
                 methodList,
                 paramList,
                 typeDefList,
                 typeRefList,
                 typeSpecList,
                 standAlongSigList)
  }
}
