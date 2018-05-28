package pravda.dotnet

import pravda.dotnet.PE.Info._
import utils._

final case class TablesData(
    fieldTable: Seq[TablesData.FieldData],
    memberRefTable: Seq[TablesData.MemberRefData],
    methodDefTable: Seq[TablesData.MethodDefData],
    paramTable: Seq[TablesData.ParamData],
    typeDefTable: Seq[TablesData.TypeDefData],
    typeRefTable: Seq[TablesData.TypeRefData],
    standAloneSigTable: Seq[TablesData.StandAloneSigData]
) {

  def tableByNum(num: Int): Option[Seq[TablesData.TableRowData]] = num match {
    case 1  => Some(typeRefTable)
    case 2  => Some(typeDefTable)
    case 4  => Some(fieldTable)
    case 6  => Some(methodDefTable)
    case 8  => Some(paramTable)
    case 10 => Some(memberRefTable)
    case 17 => Some(standAloneSigTable)
    case _  => None
  }
}

object TablesData {
  import TablesInfo._

  sealed trait TableRowData
  final case class MethodDefData(implFlags: Short,
                                 flags: Short,
                                 name: String,
                                 signatureIdx: Long,
                                 params: Seq[ParamData])
      extends TableRowData
  final case class MemberRefData(cls: Long, name: String, signatureIdx: Long) extends TableRowData
  final case class FieldData(flags: Short, name: String, signatureIdx: Long)  extends TableRowData
  final case class ParamData(flags: Short, seq: Int, name: String)            extends TableRowData
  final case class TypeDefData(flags: Int,
                               name: String,
                               namespace: String,
                               parent: TableRowData,
                               fields: Seq[FieldData],
                               methods: Seq[MethodDefData])
      extends TableRowData
  final case class TypeRefData(resolutionScopeIdx: Long, name: String, namespace: String) extends TableRowData
  final case class StandAloneSigData(signatureIdx: Long)                                  extends TableRowData
  case object Ignored                                                                     extends TableRowData

  def fromInfo(peData: PeData): Validated[TablesData] = {

    def sizesFromIds(ids: Seq[Long]): Seq[Long] =
      ids match {
        case rest :+ last =>
          val sizes = for {
            i <- rest.indices
          } yield ids(i + 1) - ids(i)
          sizes :+ (ids.length - last)
        case _ => Seq.empty
      }

    val standAlongSigList = peData.tables.standAloneSigTable.map {
      case StandAloneSigRow(blobIdx) => StandAloneSigData(blobIdx)
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

    val memberRefListV = peData.tables.memberRefTable.map {
      case MemberRefRow(cls, nameIdx, signatureIdx) =>
        for {
          name <- Heaps.string(peData.stringHeap, nameIdx)
        } yield MemberRefData(cls, name, signatureIdx)
    }.sequence

    def methodListV(paramList: Seq[ParamData]): Validated[Seq[MethodDefData]] = {
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

    def typeDefListV(fieldList: Seq[FieldData], methodList: Seq[MethodDefData]): Validated[Seq[TypeDefData]] = {
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

    for {
      paramList <- paramListV
      fieldList <- fieldListV
      memberRefList <- memberRefListV
      methodList <- methodListV(paramList)
      typeDefList <- typeDefListV(fieldList, methodList)
      typeRefList <- typeRefListV
    } yield TablesData(fieldList, memberRefList, methodList, paramList, typeDefList, typeRefList, standAlongSigList)
  }
}
