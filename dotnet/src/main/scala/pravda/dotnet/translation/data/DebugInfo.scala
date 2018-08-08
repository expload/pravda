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
