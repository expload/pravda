package pravda.dotnet

import pravda.dotnet.TablesData.TableRowData
import pravda.dotnet.utils._

object CodedIndexes {

  def memberRefParent(idx: Long,
                      typeDefTable: Seq[TablesData.TypeDefData],
                      typeRefTable: Seq[TablesData.TypeRefData],
                      typeSpecTable: Seq[TablesData.TypeSpecData]): Validated[TableRowData] = {
    val tableIdx = idx & 0x7
    val rowIdx = (idx >> 3).toInt - 1
    tableIdx match {
      case 0 => validated(typeDefTable(rowIdx))
      case 1 => validated(typeRefTable(rowIdx))
      case 4 => validated(typeSpecTable(rowIdx))
      case n => validationError(s"Unknown MemberRefParent table index: $n")
    }
  }
}
