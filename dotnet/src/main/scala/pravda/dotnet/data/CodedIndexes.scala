package pravda.dotnet.data

import pravda.dotnet.data.TablesData._

object CodedIndexes {

  def memberRefParent(idx: Long,
                      typeDefTable: Vector[TablesData.TypeDefData],
                      typeRefTable: Vector[TablesData.TypeRefData],
                      typeSpecTable: Vector[TablesData.TypeSpecData]): Either[String, TableRowData] = {
    val tableIdx = idx & 0x7
    val rowIdx = (idx >> 3).toInt - 1
    tableIdx match {
      case 0 => Right(typeDefTable(rowIdx))
      case 1 => Right(typeRefTable(rowIdx))
      case 4 => Right(typeSpecTable(rowIdx))
      case n => Left(s"Unknown MemberRefParent table index: $n")
    }
  }
}
