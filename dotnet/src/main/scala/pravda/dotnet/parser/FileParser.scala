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

package pravda.dotnet.parser

import fastparse.byte.all._
import pravda.dotnet.data.{Method, TablesData}
import pravda.dotnet.parser.CIL.CilData
import pravda.dotnet.parser.PE.Info.{Pdb, Pe}
import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._

object FileParser {

  def parsePe(bytes: Array[Byte]): Either[String, (Pe, CilData, List[Method], Map[Long, Signatures.Signature])] =
    for {
      pe <- PE.parseInfo(Bytes(bytes))
      cilData <- CIL.fromPeData(pe.peData)
      methods <- pe.methods.map(Method.parse(pe.peData, _)).sequence
      signatures <- Signatures.collectSignatures(cilData)
    } yield (pe, cilData, methods, signatures)

  def parsePdb(bytes: Array[Byte]): Either[String, (Pdb, TablesData)] =
    for {
      pdb <- PE.parsePdb(Bytes(bytes))
      tables <- TablesData.fromInfo(pdb.pdbData)
    } yield (pdb, tables)
}
