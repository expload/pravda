package io.mytc.sood.cil

import io.mytc.sood.cil.PE.Info._
import utils._
import fastparse.byte.all._

final case class TablesData(
    fieldTable: Seq[TablesData.FieldData] = Seq.empty,
    memberRefTable: Seq[TablesData.MemberRefData] = Seq.empty,
    methodDefTable: Seq[TablesData.MethodDefData] = Seq.empty,
    paramTable: Seq[TablesData.ParamData] = Seq.empty,
    typeDefTable: Seq[TablesData.TypeDefData] = Seq.empty
)

object TablesData {
  import TablesInfo._

  sealed trait TableRowData
  final case class MethodDefData(implFlags: Short, flags: Short, name: String, signature: Bytes, params: Seq[ParamData])
      extends TableRowData
  final case class MemberRefData(cls: Long, name: String, signature: Bytes) extends TableRowData
  final case class FieldData(flags: Short, name: String, signature: Bytes) extends TableRowData
  final case class ParamData(flags: Short, seq: Int, name: String) extends TableRowData
  final case class TypeDefData(flags: Int,
                               name: String,
                               namespace: String,
                               parent: TableRowData,
                               fields: Seq[FieldData],
                               methods: Seq[MethodDefData])
      extends TableRowData
  case object Ignored extends TableRowData

  def fromInfo(peData: PeData): Validated[TablesData] = {

    val paramListV = peData.tables.paramTable.map {
      case ParamRow(flags, seq, nameIdx) =>
        for {
          name <- Heaps.string(peData.stringHeap, nameIdx)
        } yield ParamData(flags, seq, name)
    }.sequence

    val fieldListV = p

    for {

    }

    val paramListIdxs = peData.tables.methodDefTable.map(_.paramListIdx)
    val paramListNextIdx = (for {
      i <- paramListIdxs.indices.dropRight(1)
    } yield paramListIdxs(i) -> paramListIdxs(i + 1)).toMap



    peData.tables
      .map(ts =>
        ts.map {
          case MethodDefRow(_, implFlags, flags, nameIdx, signatureIdx, paramListIdx) =>
            for {
              paramList <- paramListV
              name <- Heaps.string(peData.stringHeap, nameIdx)
              signature <- Heaps.blob(peData.blobHeap, signatureIdx)
              nextIdx <- paramListNextIdx
                .get(paramListIdx)
                .fold[Either[String, Long]](Left("Wrong paramList value"))(Right(_))
            } yield
              MethodDefData(implFlags,
                            flags,
                            name,
                            signature,
                            paramList.slice(paramListIdx.toInt - 1, nextIdx.toInt - 1))
          case MemberRefRow(cls, nameIdx, signatureIdx) =>
            for {
              name <- Heaps.string(peData.stringHeap, nameIdx)
              signature <- Heaps.blob(peData.blobHeap, signatureIdx)
            } yield MemberRefData(cls, name, signature)
          case FieldRow(flags, nameIdx, signatureIdx) =>
            for {
              name <- Heaps.string(peData.stringHeap, nameIdx)
              signature <- Heaps.blob(peData.blobHeap, signatureIdx)
            } yield FieldData(flags, name, signature)
          case TypeDefRow(flags, nameIdx, namespace, parent, fieldListIdx, methodListIdx) =>
          case _                                                                          => Right(Ignored)
        }.sequence)
      .sequence
  }
}
