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

package pravda.dotnet.data

import pravda.dotnet.parser.CIL
import pravda.dotnet.parser.CIL.Op
import pravda.dotnet.parser.PE.Info._
import pravda.dotnet.utils._

// See http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-335.pdf p.285
final case class Method(opcodes: List[Op], maxStack: Int, localVarSigIdx: Option[Long])

object Method {

  def parse(peData: PeData, header: MethodHeader): Either[String, Method] = {
    header match {
      case FatMethodHeader(_, _, maxStack, localVarSigTok, codeBytes) =>
        val localVarSigIdx = if ((localVarSigTok >> 24) != 17) None else Some((localVarSigTok & 0x00ffffff) - 1)
        for {
          cilData <- CIL.fromPeData(peData)
          localVarSig = localVarSigIdx.map(i => cilData.tables.standAloneSigTable(i.toInt).signatureIdx)
          codeParser = CIL.code(cilData)
          code <- codeParser.parse(header.codeBytes).toEither.joinRight
        } yield Method(code, maxStack, localVarSig)
      case TinyMethodHeader(codeBytes) =>
        for {
          cilData <- CIL.fromPeData(peData)
          codeParser = CIL.code(cilData)
          code <- codeParser.parse(header.codeBytes).toEither.joinRight
        } yield Method(code, 0, None)
      case EmptyHeader =>
        Right(Method(List.empty, 0, None))
    }
  }
}
