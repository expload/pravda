package io.mytc.sood.cil

import fastparse.byte.all._
import LE._
import io.mytc.sood.cil.utils._

object TablesInfo {

  trait Index {
    val size: Int
    val parser: P[Long]
  }
  case object ShortIndex extends Index {
    override val size: Int = 2
    override val parser: P[Long] = P(UInt16).map(_.toLong)
  }
  case object LongIndex extends Index {
    override val size: Int = 4
    override val parser: P[Long] = P(UInt32)
  }

  final case class TableIndexes(blobHeap: Index,
                                stringHeap: Index,
                                guidHeap: Index,
                                assemblyRef: Index,
                                typeDef: Index,
                                event: Index,
                                hasConstant: Index,
                                hasCustomAttribute: Index,
                                hasDeclSecurity: Index,
                                hasFieldMarshal: Index,
                                hasSemantics: Index,
                                customAttibute: Index,
                                typeDefOrRef: Index,
                                typeOrMethodDef: Index,
                                methodDefOrRef: Index,
                                implementation: Index,
                                field: Index,
                                genericParam: Index,
                                memberForwarded: Index,
                                moduleRef: Index,
                                memberRefParent: Index,
                                methodDef: Index,
                                param: Index,
                                property: Index,
                                resolutionScope: Index)

  sealed trait TableRowInfo
  case object AssemblyRow extends TableRowInfo
  case object AssemblyOSRow extends TableRowInfo
  case object AssemblyProcessorRow extends TableRowInfo
  case object AssemblyRefRow extends TableRowInfo
  case object AssemblyRefOSRow extends TableRowInfo
  case object AssemblyRefProcessorRow extends TableRowInfo

  case object ClassLayoutRow extends TableRowInfo
  case object ConstantRow extends TableRowInfo
  case object CustomAttributeRow extends TableRowInfo

  case object DeclSecurityRow extends TableRowInfo

  case object EventMapRow extends TableRowInfo
  case object EventRow extends TableRowInfo
  case object ExportedTypeRow extends TableRowInfo

  case object FieldRow extends TableRowInfo
  case object FieldLayoutRow extends TableRowInfo
  case object FieldMarshalRow extends TableRowInfo
  case object FieldRVARow extends TableRowInfo
  case object FileRow extends TableRowInfo

  case object GenericParamRow extends TableRowInfo
  case object GenericParamConstraintRow extends TableRowInfo

  case object ImplMapRow extends TableRowInfo
  case object InterfaceImplRow extends TableRowInfo

  case object ManifestResourceRow extends TableRowInfo
  case object MemberRefRow extends TableRowInfo
  final case class MethodDefRow(rva: Long,
                                implFlags: Short,
                                flags: Short,
                                nameIdx: Long,
                                signatureIdx: Long,
                                paramListIdx: Long)
      extends TableRowInfo
  case object MethodImplRow extends TableRowInfo
  case object MethodSemanticsRow extends TableRowInfo
  case object MethodSpecRow extends TableRowInfo
  case object ModuleRow extends TableRowInfo
  case object ModuleRefRow extends TableRowInfo

  case object NestedClassRow extends TableRowInfo

  case object ParamRow extends TableRowInfo
  case object PropertyRow extends TableRowInfo
  case object PropertyMapRow extends TableRowInfo

  case object StandAloneSigRow extends TableRowInfo

  case object TypeDefRow extends TableRowInfo
  case object TypeRefRow extends TableRowInfo
  case object TypeSpecRow extends TableRowInfo

  def assemblyRow(indexes: TableIndexes): P[AssemblyRow.type] =
    P(AnyBytes(4 + 4 * 2 + 4 + indexes.blobHeap.size + indexes.stringHeap.size * 2)).map(_ => AssemblyRow)
  val assemblyOSRow: P[AssemblyOSRow.type] =
    P(AnyBytes(4 * 3)).map(_ => AssemblyOSRow)
  val asssemblyProcessorRow: P[AssemblyProcessorRow.type] =
    P(AnyBytes(4)).map(_ => AssemblyProcessorRow)
  def assemblyRefRow(indexes: TableIndexes): P[AssemblyRefRow.type] =
    P(AnyBytes(4 * 2 + 4 + indexes.blobHeap.size * 2 + indexes.stringHeap.size * 2)).map(_ => AssemblyRefRow)
  def assemblyRefOSRow(indexes: TableIndexes): P[AssemblyRefOSRow.type] =
    P(AnyBytes(3 * 4 + indexes.assemblyRef.size)).map(_ => AssemblyRefOSRow)
  def assemblyRefProcessorRow(indexes: TableIndexes): P[AssemblyRefProcessorRow.type] =
    P(AnyBytes(4 + indexes.assemblyRef.size)).map(_ => AssemblyRefProcessorRow)

  def classLayoutRow(indexes: TableIndexes): P[ClassLayoutRow.type] =
    P(AnyBytes(2 + 4 + indexes.typeDef.size)).map(_ => ClassLayoutRow)
  def constantRow(indexes: TableIndexes): P[ConstantRow.type] =
    P(AnyBytes(2 + indexes.hasConstant.size + indexes.blobHeap.size)).map(_ => ConstantRow)
  def customAttributeRow(indexes: TableIndexes): P[CustomAttributeRow.type] =
    P(AnyBytes(indexes.hasCustomAttribute.size + indexes.customAttibute.size + indexes.blobHeap.size))
      .map(_ => CustomAttributeRow)

  def declSecurityRow(indexes: TableIndexes): P[DeclSecurityRow.type] =
    P(AnyBytes(2 + indexes.hasDeclSecurity.size + indexes.blobHeap.size)).map(_ => DeclSecurityRow)

  def eventMapRow(indexes: TableIndexes): P[EventMapRow.type] =
    P(AnyBytes(indexes.typeDef.size + indexes.event.size)).map(_ => EventMapRow)
  def eventRow(indexes: TableIndexes): P[EventRow.type] =
    P(AnyBytes(2 + indexes.stringHeap.size + indexes.typeDefOrRef.size)).map(_ => EventRow)
  def exportedTypeRow(indexes: TableIndexes): P[ExportedTypeRow.type] =
    P(AnyBytes(4 + 4 + indexes.stringHeap.size + indexes.stringHeap.size + indexes.implementation.size))
      .map(_ => ExportedTypeRow)

  def fieldRow(indexes: TableIndexes): P[FieldRow.type] =
    P(AnyBytes(2 + indexes.stringHeap.size + indexes.blobHeap.size)).map(_ => FieldRow)
  def fieldLayoutRow(indexes: TableIndexes): P[FieldLayoutRow.type] =
    P(AnyBytes(4 + indexes.field.size)).map(_ => FieldLayoutRow)
  def fieldMarshalRow(indexes: TableIndexes): P[FieldMarshalRow.type] =
    P(AnyBytes(indexes.hasFieldMarshal.size + indexes.blobHeap.size)).map(_ => FieldMarshalRow)
  def fieldRVARow(indexes: TableIndexes): P[FieldRVARow.type] =
    P(AnyBytes(4 + indexes.field.size)).map(_ => FieldRVARow)
  def fileRow(indexes: TableIndexes): P[FileRow.type] =
    P(AnyBytes(4 + indexes.stringHeap.size + indexes.blobHeap.size)).map(_ => FileRow)

  def genericParamRow(indexes: TableIndexes): P[GenericParamRow.type] =
    P(AnyBytes(2 + 2 + indexes.typeOrMethodDef.size + indexes.stringHeap.size)).map(_ => GenericParamRow)
  def genericParamConstraintRow(indexes: TableIndexes): P[GenericParamConstraintRow.type] =
    P(AnyBytes(indexes.genericParam.size + indexes.typeDefOrRef.size)).map(_ => GenericParamConstraintRow)

  def implMapRow(indexes: TableIndexes): P[ImplMapRow.type] =
    P(AnyBytes(2 + indexes.memberForwarded.size + indexes.stringHeap.size + indexes.moduleRef.size))
      .map(_ => ImplMapRow)
  def interfaceImplRow(indexes: TableIndexes): P[InterfaceImplRow.type] =
    P(AnyBytes(indexes.typeDef.size + indexes.typeDefOrRef.size)).map(_ => InterfaceImplRow)

  def manifestResourceRow(indexes: TableIndexes): P[ManifestResourceRow.type] =
    P(AnyBytes(4 + 4 + indexes.stringHeap.size + indexes.implementation.size)).map(_ => ManifestResourceRow)
  def memberRefRow(indexes: TableIndexes): P[MemberRefRow.type] =
    P(AnyBytes(indexes.memberRefParent.size + indexes.stringHeap.size + indexes.blobHeap.size))
      .map(_ => MemberRefRow)
  def methodDefRow(indexes: TableIndexes): P[MethodDefRow] = {
    val rva = UInt32
    val implFlags = Int16
    val flags = Int16
    val nameIdx = indexes.stringHeap.parser
    val signatureIdx = indexes.blobHeap.parser
    val paramListIdx = indexes.param.parser
    P(rva ~ implFlags ~ flags ~ nameIdx ~ signatureIdx ~ paramListIdx).map(MethodDefRow.tupled)
  }
  def methodImplRow(indexes: TableIndexes): P[MethodImplRow.type] =
    P(AnyBytes(indexes.typeDef.size + indexes.methodDefOrRef.size * 2)).map(_ => MethodImplRow)
  def methodSemanticsRow(indexes: TableIndexes): P[MethodSemanticsRow.type] =
    P(AnyBytes(2 + indexes.methodDef.size + indexes.hasSemantics.size)).map(_ => MethodSemanticsRow)
  def methodSpecRow(indexes: TableIndexes): P[MethodSpecRow.type] =
    P(AnyBytes(indexes.methodDefOrRef.size + indexes.blobHeap.size)).map(_ => MethodSpecRow)
  def moduleRow(indexes: TableIndexes): P[ModuleRow.type] =
    P(AnyBytes(2 + indexes.stringHeap.size + indexes.guidHeap.size * 3)).map(_ => ModuleRow)
  def moduleRefRow(indexes: TableIndexes): P[ModuleRefRow.type] =
    P(AnyBytes(indexes.stringHeap.size)).map(_ => ModuleRefRow)

  def nestedClassRow(indexes: TableIndexes): P[NestedClassRow.type] =
    P(AnyBytes(indexes.typeDef.size * 2)).map(_ => NestedClassRow)

  def paramRow(indexes: TableIndexes): P[ParamRow.type] =
    P(AnyBytes(2 + 2 + indexes.stringHeap.size)).map(_ => ParamRow)
  def propertyRow(indexes: TableIndexes): P[PropertyRow.type] =
    P(AnyBytes(2 + indexes.stringHeap.size + indexes.blobHeap.size)).map(_ => PropertyRow)
  def propertyMapRow(indexes: TableIndexes): P[PropertyMapRow.type] =
    P(AnyBytes(indexes.typeDef.size + indexes.property.size)).map(_ => PropertyMapRow)

  def standAloneSigRow(indexes: TableIndexes): P[StandAloneSigRow.type] =
    P(AnyBytes(indexes.blobHeap.size)).map(_ => StandAloneSigRow)

  def typeDefRow(indexes: TableIndexes): P[TypeDefRow.type] =
    P(
      AnyBytes(
        4 + indexes.stringHeap.size * 2 + indexes.typeDefOrRef.size + indexes.field.size + indexes.methodDef.size))
      .map(_ => TypeDefRow)
  def typeRefRow(indexes: TableIndexes): P[TypeRefRow.type] =
    P(AnyBytes(indexes.resolutionScope.size + indexes.stringHeap.size * 2)).map(_ => TypeRefRow)
  def typeSpecRow(indexes: TableIndexes): P[TypeSpecRow.type] =
    P(AnyBytes(indexes.blobHeap.size)).map(_ => TypeSpecRow)

  val numToParser: Map[Int, TableIndexes => P[TableRowInfo]] = Map(
    0 -> moduleRow,
    1 -> typeRefRow,
    2 -> typeDefRow,
    // 3
    4 -> fileRow,
    // 5
    6 -> methodDefRow,
    // 7
    8 -> paramRow,
    9 -> interfaceImplRow,
    10 -> memberRefRow,
    11 -> constantRow,
    12 -> customAttributeRow,
    13 -> fieldMarshalRow,
    14 -> declSecurityRow,
    15 -> classLayoutRow,
    16 -> fieldLayoutRow,
    17 -> standAloneSigRow,
    18 -> eventMapRow,
    // 19
    20 -> eventRow,
    21 -> propertyMapRow,
    // 22
    23 -> propertyRow,
    24 -> methodSemanticsRow,
    25 -> methodImplRow,
    26 -> moduleRefRow,
    27 -> typeSpecRow,
    28 -> implMapRow,
    29 -> fieldRVARow,
    // 30
    // 31
    32 -> assemblyRow,
    33 -> (_ => asssemblyProcessorRow),
    34 -> (_ => assemblyOSRow),
    35 -> assemblyRefRow,
    36 -> assemblyRefProcessorRow,
    37 -> assemblyRefOSRow,
    38 -> fileRow,
    39 -> exportedTypeRow,
    40 -> manifestResourceRow,
    41 -> nestedClassRow,
    42 -> genericParamRow,
    43 -> methodSpecRow,
    44 -> genericParamConstraintRow
  )

  def validToActualTableNumbers(valid: Long): Seq[Int] =
    valid.toBinaryString.reverse.zipWithIndex.filter(_._1 == '1').map(_._2)

  private def tableIndexes(heapSizes: Byte, tableNumbers: Seq[Int], rows: Seq[Long]): TableIndexes = {
    TableIndexes(
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex,
      ShortIndex
    ) // FIXME
  }

  def tables(heapSizes: Byte, tableNumbers: Seq[Int], rows: Seq[Long]): P[Validated[Seq[Seq[TableRowInfo]]]] = {
    val indexes = tableIndexes(heapSizes, tableNumbers, rows)
    val parsers = tableNumbers.map(numToParser.get)

    if (parsers.exists(_.isEmpty)) {
      PassWith(validationError("Non valid tables numbers"))
    } else {
      if (parsers.length != rows.length) {
        PassWith(validationError("Inconsistent number of tables"))
      } else {
        parsers.flatten
          .zip(rows)
          .map {
            case (p, size) => p(indexes).rep(exactly = size.toInt /* should be enough, probably... */ )
          }
          .foldLeft[P[Seq[Seq[TableRowInfo]]]](PassWith(Seq.empty[Seq[TableRowInfo]])) {
            case (p, nextP) =>
              (p ~ nextP).map {
                case (tables, newTable) => tables :+ newTable
              }
          }
          .map(validated)
      }
    }
  }

}
