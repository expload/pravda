package pravda.dotnet.translation.data

import pravda.dotnet.data.TablesData.TypeDefData
import pravda.dotnet.parser.FileParser.ParsedDotnetFile
import pravda.dotnet.translation.NamesBuilder


/**
  * Inverted index for TypeDef component
  *
  * @param all all components from give TypeDefs
  * @param fromNames
  * @param parents
  */
final case class TypeDefInvertedIndex[T](all: Vector[T], fromNames: Map[String, Int], parents: Map[Int, TypeDefData]) {

  def apply(i: Int): T = all(i)

  def parent(i: Int): Option[TypeDefData] = parents.get(i)

  def byName(name: String): Option[T] = fromNames.get(name).map(all)

  private[data] def merge(other: TypeDefInvertedIndex[T]): TypeDefInvertedIndex[T] =
    new TypeDefInvertedIndex[T](all ++ other.all, fromNames ++ other.fromNames, parents ++ other.parents)
}

object TypeDefInvertedIndex {

  def apply[T](typeDefs: Seq[TypeDefData],
               fromTypeDef: TypeDefData => Seq[T],
               name: T => String): TypeDefInvertedIndex[T] = apply(typeDefs, fromTypeDef, name)

  private[data] def apply[T](typeDefs: Seq[TypeDefData],
                             fromTypeDef: TypeDefData => Seq[T],
                             name: T => String,
                             offset: Int): TypeDefInvertedIndex[T] = {
    val withTd = typeDefs.flatMap { td => fromTypeDef(td).map(t => t -> td)
    }

    val (all, names, parents) = withTd.zipWithIndex.map {
      case ((t, td), i) =>
        (t, s"${NamesBuilder.fullTypeDef(td)}.${name(t)}" -> i, i -> td)
    }.unzip3

    new TypeDefInvertedIndex[T](all.toVector, names.toMap, parents.toMap)
  }
}

final case class TypeDefInvertedFileIndex[T](index: TypeDefInvertedIndex[T], indexIdx: Vector[Int]) {

  def apply(fileIdx: Int): T = index(indexIdx(fileIdx))

  def parent(fileIdx: Int): Option[TypeDefData] = index.parents.get(indexIdx(fileIdx))

  def byName(name: String): Option[T] = index.fromNames.get(name).map(apply)
}

object TypeDefInvertedFileIndex {

  def apply[T](files: Seq[ParsedDotnetFile],
               fromTypeDef: TypeDefData => Seq[T],
               name: (ParsedDotnetFile, T) => String): Seq[TypeDefInvertedFileIndex[T]] = {
    val (index, fileIndices, _) = files.foldLeft[(TypeDefInvertedIndex[T], List[Vector[Int]], Int)](
      (new TypeDefInvertedIndex[T](Vector.empty, Map.empty, Map.empty), List.empty, 0)) {
      case ((acc, fileIdxes, total), f) =>
        val index = TypeDefInvertedIndex[T](f.parsedPe.cilData.tables.typeDefTable, fromTypeDef, t => name(f, t), total)
        (acc.merge(index), total.to(total + index.all.size).toVector :: fileIdxes, total + index.all.size)
    }

    fileIndices.map(fi => new TypeDefInvertedFileIndex[T](index, fi))
  }
}
