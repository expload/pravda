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

import pravda.dotnet.data.Heaps
import pravda.dotnet.data.TablesData.MethodDebugInformationData
import pravda.vm.Meta

object DebugInfo {

  def searchForSourceMarks(debugInfo: MethodDebugInformationData,
                           cilOffsetStart: Int,
                           cilOffsetEnd: Int): List[Meta.SourceMark] =
    debugInfo.points.filter(p => p.ilOffset >= cilOffsetStart && p.ilOffset < cilOffsetEnd).map {
      case Heaps.SequencePoint(_, sl, sc, el, ec) =>
        Meta.SourceMark(debugInfo.document.getOrElse("cs file"), sl, sc, el, ec)
    }

  def firstSourceMark(debugInfo: MethodDebugInformationData, ilOffset: Int): Option[Meta.SourceMark] = {
    debugInfo.points.reverse.collectFirst {
      case Heaps.SequencePoint(il, sl, sc, el, ec) if il <= ilOffset =>
        Meta.SourceMark(debugInfo.document.getOrElse("cs file"), sl, sc, el, ec)
    }
  }
}
