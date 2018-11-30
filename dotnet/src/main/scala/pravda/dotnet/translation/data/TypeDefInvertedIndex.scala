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

package pravda.dotnet.translation.data

import pravda.dotnet.data.TablesData.TypeDefData
import pravda.dotnet.parser.FileParser.ParsedDotnetFile
import pravda.dotnet.translation.NamesBuilder

/**
  * Inverted index for `TypeDef` component
  *
  * @param all all components from given `TypeDef`s
  * @param fromNames a map from full name of component to the index in `all`
  * @param parents a map from index in `all` to the parent `TypeDef`
  */
final case class TypeDefInvertedIndex[T](all: Vector[T], fromNames: Map[String, Int], parents: Map[Int, TypeDefData]) {

  def apply(i: Int): T = all(i)

  def parent(i: Int): Option[TypeDefData] = parents.get(i)

  def byName(name: String): Option[(TypeDefData, T)] =
    for {
      idx <- fromNames.get(name)
      p <- parents.get(idx)
    } yield (p, all(idx))

  private[data] def merge(other: TypeDefInvertedIndex[T]): TypeDefInvertedIndex[T] =
    new TypeDefInvertedIndex[T](all ++ other.all, fromNames ++ other.fromNames, parents ++ other.parents)
}

object TypeDefInvertedIndex {

  /**
    * Constructs inverted index from `TypeDef`s
    *
    * @param typeDefs
    * @param fromTypeDef a function to retrieve components from `TypeDef`
    * @param name a function to construct component specific prefix,
    *             this prefix is appended to full name of the parent `TypeDef`
    * @return
    */
  def apply[T](typeDefs: Seq[TypeDefData],
               fromTypeDef: TypeDefData => Seq[T],
               name: T => String): TypeDefInvertedIndex[T] = apply(typeDefs, fromTypeDef, name)

  private[data] def apply[T](typeDefs: Seq[TypeDefData],
                             fromTypeDef: TypeDefData => Seq[T],
                             name: T => String,
                             offset: Int): TypeDefInvertedIndex[T] = {
    val withTd = typeDefs.flatMap { td =>
      fromTypeDef(td).map(t => t -> td)
    }

    val (all, names, parents) = withTd.zipWithIndex.map {
      case ((t, td), i) =>
        (t, s"${NamesBuilder.fullTypeDef(td)}.${name(t)}" -> (i + offset), (i + offset) -> td)
    }.unzip3

    new TypeDefInvertedIndex[T](all.toVector, names.toMap, parents.toMap)
  }
}

/**
  * `TypeDefInvertedIndex` with additional indices indicating components only from one file
  *
  * @param index Global inverted index
  * @param indexIdx Indices of components from `index.all` that belong to one file
  */
final case class TypeDefInvertedFileIndex[T](index: TypeDefInvertedIndex[T], indexIdx: Vector[Int]) {

  def apply(fileIdx: Int): T = index(indexIdx(fileIdx))

  def parent(fileIdx: Int): Option[TypeDefData] = index.parents.get(indexIdx(fileIdx))

  def byName(name: String): Option[(TypeDefData, T)] = index.byName(name)
}

object TypeDefInvertedFileIndex {

  /**
    * Constructs sequence of `TypeDefInvertedIndex` for each given file
    *
    * @param fromTypeDef a function to retrieve components from `TypeDef`, similar to function from `TypeDefInvertedIndex.apply`
    * @param name a function to construct component specific prefix
    * @return
    */
  def apply[T](files: Seq[ParsedDotnetFile],
               fromTypeDef: TypeDefData => Seq[T],
               name: (ParsedDotnetFile, T) => String): Seq[TypeDefInvertedFileIndex[T]] = {
    val (index, fileIndices, _) = files.foldLeft[(TypeDefInvertedIndex[T], List[Vector[Int]], Int)](
      (new TypeDefInvertedIndex[T](Vector.empty, Map.empty, Map.empty), List.empty, 0)) {
      case ((acc, fileIdxes, total), f) =>
        val index =
          TypeDefInvertedIndex[T](f.parsedPe.cilData.tables.typeDefTable, fromTypeDef, (t: T) => name(f, t), total)
        (acc.merge(index), total.until(total + index.all.size).toVector :: fileIdxes, total + index.all.size)
    }

    fileIndices.reverse.map(fi => new TypeDefInvertedFileIndex[T](index, fi))
  }
}
