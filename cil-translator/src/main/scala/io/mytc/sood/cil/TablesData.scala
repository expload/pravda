package io.mytc.sood.cil

import io.mytc.sood.cil.PE.Info._
import utils._
import fastparse.byte.all._

final case class TablesData(
    fieldTable: Seq[TablesData.FieldData] = Seq.empty,
    memberRefTable: Seq[TablesData.MemberRefData] = Seq.empty,
    methodDefTable: Seq[TablesData.MethodDefData] = Seq.empty,
    paramTable: Seq[TablesData.ParamData] = Seq.empty,
    typeDefTable: Seq[TablesData.TypeDefData] = Seq.empty,
    standAloneSigTable: Seq[TablesData.StandAloneSigData] = Seq.empty
) {

  def tableByNum(num: Int): Option[Seq[TablesData.TableRowData]] = num match {
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
  final case class MethodDefData(implFlags: Short, flags: Short, name: String, signature: Bytes, params: Seq[ParamData])
      extends TableRowData
  final case class MemberRefData(cls: Long, name: String, signature: Bytes) extends TableRowData
  final case class FieldData(flags: Short, name: String, signature: Bytes)  extends TableRowData
  final case class ParamData(flags: Short, seq: Int, name: String)          extends TableRowData
  final case class TypeDefData(flags: Int,
                               name: String,
                               namespace: String,
                               parent: TableRowData,
                               fields: Seq[FieldData],
                               methods: Seq[MethodDefData])
      extends TableRowData
  final case class StandAloneSigData(signature: Signatures.Signature) extends TableRowData
  case object Ignored                                                      extends TableRowData

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

    val standAlongSigListV = peData.tables.standAloneSigTable.map {
      case StandAloneSigRow(blobIdx) =>
        for {
          localVarSigBytes <- Heaps.blob(peData.blobHeap, blobIdx)
          localVarSig <- Signatures.localVarSig.parse(localVarSigBytes).toValidated
        } yield StandAloneSigData(localVarSig)
    }.sequence

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
          signature <- Heaps.blob(peData.blobHeap, signatureIdx)
        } yield FieldData(flags, name, signature)
    }.sequence

    val memberRefListV = peData.tables.memberRefTable.map {
      case MemberRefRow(cls, nameIdx, signatureIdx) =>
        for {
          name <- Heaps.string(peData.stringHeap, nameIdx)
          signature <- Heaps.blob(peData.blobHeap, signatureIdx)
        } yield MemberRefData(cls, name, signature)
    }.sequence

    def methodListV(paramList: Seq[ParamData]): Validated[Seq[MethodDefData]] = {
      val paramListSizes = sizesFromIds(peData.tables.methodDefTable.map(_.paramListIdx))

      peData.tables.methodDefTable.zipWithIndex.map {
        case (MethodDefRow(_, implFlags, flags, nameIdx, signatureIdx, paramListIdx), i) =>
          for {
            name <- Heaps.string(peData.stringHeap, nameIdx)
            signature <- Heaps.blob(peData.blobHeap, signatureIdx)
          } yield
            MethodDefData(implFlags,
                          flags,
                          name,
                          signature,
                          paramList.slice(paramListIdx.toInt - 1, paramListIdx.toInt + paramListSizes(i).toInt - 1))
      }.sequence
    }

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
      standAlongSigList <- standAlongSigListV
    } yield TablesData(fieldList, memberRefList, methodList, paramList, typeDefList, standAlongSigList)
  }
}
