package io.mytc.sood.cil

import io.mytc.sood.cil.PE.Info._

object TablesData {
  import TablesInfo._

  sealed trait TableRowData
  final case class MethodDefData(name: String) extends TableRowData
  case object Ignored extends TableRowData

  def fromInfo(peData: PeData): Either[String, Seq[Seq[TableRowData]]] = {
    utils.sequenceEither(peData.tables.map(ts =>
      utils.sequenceEither(ts.map {
        case MethodDefRow(_, _, _, nameIdx, _, _) => Heaps.string(peData.stringHeap, nameIdx).map(MethodDefData)
        case _ => Right(Ignored)
      })))
  }
}
